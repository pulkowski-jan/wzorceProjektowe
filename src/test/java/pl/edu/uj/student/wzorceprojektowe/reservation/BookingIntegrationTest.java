package pl.edu.uj.student.wzorceprojektowe.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pl.edu.uj.student.wzorceprojektowe.offer.repository.OfferRepository;
import pl.edu.uj.student.wzorceprojektowe.reservation.model.CreateReservationRequest;
import pl.edu.uj.student.wzorceprojektowe.reservation.repository.ReservationRepository;
import pl.edu.uj.student.wzorceprojektowe.reservation.service.ReservationService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Real HTTP-to-SQLite tests, including concurrent requests on separate pooled connections. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite:build/booking-test-${random.uuid}.db",
    "spring.datasource.hikari.connection-init-sql=PRAGMA foreign_keys = ON",
    "spring.datasource.hikari.data-source-properties.busy_timeout=10000",
    "spring.datasource.hikari.maximum-pool-size=8"
})
class BookingIntegrationTest {
  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private JsonMapper mapper;
  @Autowired private OfferRepository offers;
  @Autowired private ReservationRepository reservations;
  @Autowired private ReservationService service;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
    jdbc.update("DELETE FROM payment");
    jdbc.update("DELETE FROM reservation");
    jdbc.update("DELETE FROM client");
    jdbc.update("DELETE FROM offer");
    jdbc.update("DELETE FROM vendor");
  }

  @Test
  void clientCrudAndGeneratedIdentity() throws Exception {
    JsonNode client = response(post("/clients"), """
        {"id":"untrusted-id","name":"  Buyer  ","email":" buyer@example.org "}
        """, 200);
    String id = client.get("id").asString();
    assertThat(UUID.fromString(id)).isNotNull();
    assertThat(client.get("name").asString()).isEqualTo("Buyer");
    assertThat(client.get("email").asString()).isEqualTo("buyer@example.org");
    assertThat(response(get("/clients/{id}", id), null, 200)).isEqualTo(client);
    assertThat(response(get("/clients"), null, 200).get(0)).isEqualTo(client);
    JsonNode updated = response(put("/clients/{id}", id),
        "{\"name\":\"Company\",\"email\":\"company@example.org\"}", 200);
    assertThat(updated.get("id").asString()).isEqualTo(id);
    assertThat(response(get("/clients/{id}", id), null, 200)).isEqualTo(updated);
    assertThat(response(delete("/clients/{id}", id), null, 200)).isEqualTo(updated);
    response(get("/clients/{id}", id), null, 404);
    response(delete("/clients/{id}", id), null, 404);
  }

  @Test
  void createResponsesFollowExistingControllerConventions() throws Exception {
    var clientResult = mvc.perform(post("/clients").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Buyer\",\"email\":\"buyer@example.org\"}"))
        .andExpect(status().isOk()).andReturn().getResponse();
    String id = mapper.readTree(clientResult.getContentAsString()).get("id").asString();
    assertThat(clientResult.getHeader("Location")).isNull();
    assertThat(UUID.fromString(id)).isNotNull();
    String offer = createOffer(2);
    var reservationResult = mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON)
            .content(basket(id, item(offer, 1))))
        .andExpect(status().isOk()).andReturn().getResponse();
    String reservationId = mapper.readTree(reservationResult.getContentAsString()).get("id").asString();
    assertThat(reservationResult.getHeader("Location")).isNull();
    assertThat(UUID.fromString(reservationId)).isNotNull();
  }

  @ParameterizedTest
  @MethodSource("invalidClients")
  void invalidClientDataReturns400WithoutWriting(String payload) throws Exception {
    response(post("/clients"), payload, 400);
    assertThat(count("client")).isZero();
  }

  static Stream<String> invalidClients() {
    return Stream.of("null", "{}", "{\"name\":\"Buyer\"}",
        "{\"name\":\" \",\"email\":\"buyer@example.org\"}",
        "{\"name\":null,\"email\":\"buyer@example.org\"}",
        "{\"name\":\"Buyer\",\"email\":\"invalid\"}",
        "{\"name\":\"Buyer\",\"email\":\"a b@example.org\"}",
        "{\"name\":\"Buyer\",\"email\":null}",
        "{\"name\":\"" + "x".repeat(256) + "\",\"email\":\"buyer@example.org\"}");
  }

  @Test
  void invalidUpdatePreservesClientAndMissingClientIs404() throws Exception {
    String id = createClient();
    JsonNode before = response(get("/clients/{id}", id), null, 200);
    response(put("/clients/{id}", id), "{\"name\":\"New\",\"email\":\"broken\"}", 400);
    assertThat(response(get("/clients/{id}", id), null, 200)).isEqualTo(before);
    response(put("/clients/{id}", UUID.randomUUID()),
        "{\"name\":\"Buyer\",\"email\":\"buyer@example.org\"}", 404);
    response(get("/clients/not-a-uuid"), null, 404);
  }

  @Test
  void duplicateEmailsAreAllowedAsDocumentationDoesNotMakeThemUnique() throws Exception {
    assertThat(createClient()).isNotEqualTo(createClient());
    assertThat(count("client")).isEqualTo(2);
  }

  @Test
  void reservationRoundTripPersistsSnapshotAndTimestamps() throws Exception {
    String client = createClient();
    String offer = createOffer(5);
    JsonNode original = response(get("/offer/{id}", offer), null, 200);
    JsonNode created = response(post("/reservations"), basket(client, """
        {"offerId":"%s","quantity":2}
        """.formatted(offer)), 200);
    String id = created.get("id").asString();
    assertThat(UUID.fromString(id)).isNotNull();
    assertThat(created.get("clientId").asString()).isEqualTo(client);
    JsonNode line = created.get("items").get(0);
    assertThat(UUID.fromString(line.get("lineId").asString())).isNotNull();
    assertThat(line.get("offerSnapshot")).isEqualTo(original);
    assertThat(line.get("offerSnapshot").get("price").get("amount").asString()).isEqualTo("1.01");
    assertThat(line.get("quantity").asInt()).isEqualTo(2);
    assertThat(line.has("status")).isFalse();
    assertThat(line.has("participant")).isFalse();
    assertThat(line.get("timestamp")).isEqualTo(created.get("createTimestamp"));
    assertThat(created.get("createTimestamp").isString()).isTrue();
    assertThat(Instant.parse(created.get("createTimestamp").asString())).isNotNull();
    assertThat(created.get("lastModifiedTimestamp")).isEqualTo(created.get("createTimestamp"));
    assertThat(response(get("/reservations/{id}", id), null, 200)).isEqualTo(created);
    assertThat(reservations.findById(id).orElseThrow().items()).hasSize(1);
    assertThat(mapper.readTree(jdbc.queryForObject(
        "SELECT offer_items_data FROM reservation WHERE id = ?", String.class, id)))
        .isEqualTo(created.get("items"));
    assertThat(offers.findById(offer).orElseThrow().quantity()).isEqualTo(3);
    assertThat(offers.findById(offer).orElseThrow().version()).isEqualTo(2);
    assertThat(jdbc.queryForObject("SELECT version FROM reservation WHERE id = ?", Integer.class, id))
        .isEqualTo(1);
  }

  @Test
  void snapshotsSurviveCatalogueUpdatesAndDeletion() throws Exception {
    String offer = createOffer(4);
    JsonNode created = response(post("/reservations"), basket(createClient(), item(offer, 1)), 200);
    response(post("/offer/update"), """
        {"id":"%s","name":"Changed","price":{"amount":"99.00","currency":"EUR"}}
        """.formatted(offer), 200);
    response(delete("/offer/{id}", offer), null, 200);
    assertThat(response(get("/reservations/{id}", created.get("id").asString()), null, 200))
        .isEqualTo(created);
  }

  @Test
  void repeatedOffersKeepSeparateLinesButUseOneSnapshotAndStockDeduction() throws Exception {
    String offer = createOffer(5);
    JsonNode created = response(post("/reservations"),
        basket(createClient(), item(offer, 2) + "," + item(offer, 3)), 200);
    JsonNode lines = created.get("items");
    assertThat(lines.size()).isEqualTo(2);
    assertThat(lines.get(0).get("lineId")).isNotEqualTo(lines.get(1).get("lineId"));
    assertThat(lines.get(0).get("offerSnapshot")).isEqualTo(lines.get(1).get("offerSnapshot"));
    assertThat(lines.get(0).has("participant")).isFalse();
    assertThat(offers.findById(offer).orElseThrow().quantity()).isZero();
    assertThat(offers.findById(offer).orElseThrow().version()).isEqualTo(2);
  }

  @Test
  void basketCanContainMultipleOffers() throws Exception {
    String first = createOffer(4);
    String second = createOffer(3);
    JsonNode created = response(post("/reservations"), basket(createClient(),
        item(first, 2) + "," + """
            {"offerId":"%s","quantity":1}
            """.formatted(second)), 200);
    assertThat(created.get("items").size()).isEqualTo(2);
    assertThat(offers.findById(first).orElseThrow().quantity()).isEqualTo(2);
    assertThat(offers.findById(second).orElseThrow().quantity()).isEqualTo(2);
    assertThat(response(get("/reservations/{id}", created.get("id").asString()), null, 200))
        .isEqualTo(created);
  }

  @Test
  void inventoryDeductionInvalidatesStaleCatalogueWrites() throws Exception {
    String id = createOffer(3);
    var stale = offers.findById(id).orElseThrow();
    response(post("/reservations"), basket(createClient(), item(id, 1)), 200);
    assertThatThrownBy(() -> offers.update(stale)).isInstanceOf(OptimisticLockingFailureException.class);
    assertThat(offers.findById(id).orElseThrow().quantity()).isEqualTo(2);
  }

  @Test
  void clientFilteringAndDeletionProtection() throws Exception {
    String client = createClient();
    String other = createClient();
    String offer = createOffer(3);
    JsonNode reservation = response(post("/reservations"), basket(client, item(offer, 1)), 200);
    response(post("/reservations"), basket(other, item(offer, 1)), 200);
    JsonNode found = response(get("/reservations").param("clientId", client), null, 200);
    assertThat(found.size()).isEqualTo(1);
    assertThat(found.get(0)).isEqualTo(reservation);
    response(delete("/clients/{id}", client), null, 409);
    response(get("/clients/{id}", client), null, 200);
    response(get("/reservations").param("clientId", UUID.randomUUID().toString()), null, 404);
    response(get("/reservations"), null, 400);
    response(get("/reservations/not-a-uuid"), null, 404);
    response(get("/reservations/{id}", UUID.randomUUID()), null, 404);
    assertThat(response(get("/reservations").param("clientId", createClient()), null, 200).size())
        .isZero();
  }

  @ParameterizedTest
  @MethodSource("invalidBaskets")
  void invalidBasketReturns400AndDoesNotWrite(String template) throws Exception {
    String client = createClient();
    String offer = createOffer(5);
    response(post("/reservations"), template.replace("CLIENT", client).replace("OFFER", offer), 400);
    assertThat(count("reservation")).isZero();
    assertThat(offers.findById(offer).orElseThrow().quantity()).isEqualTo(5);
  }

  static Stream<String> invalidBaskets() {
    return Stream.of(
        "null", "{}", "{\"clientId\":\"CLIENT\",\"items\":null}",
        "{\"clientId\":\"CLIENT\",\"items\":[]}",
        "{\"items\":[{\"offerId\":\"OFFER\",\"quantity\":1}]}",
        basket("bad-id", item("OFFER", 1)),
        basket("CLIENT", "null"),
        basket("CLIENT", "{}"),
        basket("CLIENT", "{\"offerId\":\"OFFER\"}"),
        basket("CLIENT", item("bad-id", 1)),
        basket("CLIENT", item("OFFER", 0)),
        basket("CLIENT", item("OFFER", -1)),
        basket("CLIENT", "{\"offerId\":\"OFFER\",\"quantity\":1.5}"),
        basket("CLIENT", "{\"offerId\":\"OFFER\",\"quantity\":\"1\"}"),
        basket("CLIENT", "{\"offerId\":\"OFFER\",\"quantity\":true}"),
        basket("CLIENT", "{\"offerId\":\"OFFER\",\"quantity\":2147483648}"),
        basket("CLIENT", item("OFFER", Integer.MAX_VALUE) + "," + item("OFFER", 1)));
  }

  @Test
  void unknownClientAndOfferReturn404WithoutConsumingInventory() throws Exception {
    String offer = createOffer(2);
    response(post("/reservations"), basket(UUID.randomUUID().toString(), item(offer, 1)), 404);
    response(post("/reservations"), basket(createClient(), item(UUID.randomUUID().toString(), 1)), 404);
    assertThat(count("reservation")).isZero();
    assertThat(offers.findById(offer).orElseThrow().quantity()).isEqualTo(2);
  }

  @Test
  void insufficientStockAndDuplicateLineOverbookingReturn409() throws Exception {
    String client = createClient();
    String offer = createOffer(3);
    response(post("/reservations"), basket(client, item(offer, 4)), 409);
    response(post("/reservations"), basket(client, item(offer, 2) + "," + item(offer, 2)), 409);
    assertThat(count("reservation")).isZero();
    assertThat(offers.findById(offer).orElseThrow().quantity()).isEqualTo(3);
  }

  @Test
  void failureOnLaterLineRollsBackEarlierStockAndReservation() throws Exception {
    String client = createClient();
    String first = createOffer(5);
    String second = createOffer(1);
    var before = offers.findById(first).orElseThrow();
    response(post("/reservations"), basket(client, item(first, 2) + "," + item(second, 2)), 409);
    response(post("/reservations"),
        basket(client, item(first, 2) + "," + item(UUID.randomUUID().toString(), 1)), 404);
    assertThat(offers.findById(first).orElseThrow()).isEqualTo(before);
    assertThat(offers.findById(second).orElseThrow().quantity()).isEqualTo(1);
    assertThat(count("reservation")).isZero();
  }

  @Test
  void failedJsonPersistenceRollsBackStockAndDraftReservation() throws Exception {
    String client = createClient();
    String offer = createOffer(2);
    jdbc.execute("""
        CREATE TRIGGER fail_basket BEFORE UPDATE OF offer_items_data ON reservation
        BEGIN SELECT RAISE(ABORT, 'test failure'); END
        """);
    try {
      var request = new CreateReservationRequest(client,
          List.of(new CreateReservationRequest.Item(offer, 1)));
      assertThatThrownBy(() -> service.create(request)).isInstanceOf(DataAccessException.class);
      assertThat(count("reservation")).isZero();
      assertThat(offers.findById(offer).orElseThrow().quantity()).isEqualTo(2);
      assertThat(offers.findById(offer).orElseThrow().version()).isEqualTo(1);
    } finally {
      jdbc.execute("DROP TRIGGER fail_basket");
    }
  }

  @Test
  void foreignKeysPreventOrphanReservations() {
    assertThat(jdbc.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);
    assertThatThrownBy(() -> jdbc.update("""
        INSERT INTO reservation
          (id, version, client_id, offer_items_data, create_timestamp, last_modified_timestamp)
        VALUES (?, 1, ?, '[]', 0, 0)
        """, UUID.randomUUID().toString(), UUID.randomUUID().toString()))
        .isInstanceOf(DataAccessException.class);
  }

  @Test
  void concurrentClientDeletionAndReservationNeverLeaveAnOrphan() throws Exception {
    String client = createClient();
    String offer = createOffer(1);
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Future<Integer> booking = executor.submit(() -> {
        start.await(10, TimeUnit.SECONDS);
        return mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON)
            .content(basket(client, item(offer, 1)))).andReturn().getResponse().getStatus();
      });
      Future<Integer> deletion = executor.submit(() -> {
        start.await(10, TimeUnit.SECONDS);
        return mvc.perform(delete("/clients/{id}", client)).andReturn().getResponse().getStatus();
      });
      start.countDown();
      int bookingStatus = booking.get(20, TimeUnit.SECONDS);
      int deletionStatus = deletion.get(20, TimeUnit.SECONDS);
      if (bookingStatus == 200) {
        assertThat(deletionStatus).isEqualTo(409);
        assertThat(count("client")).isEqualTo(1);
        assertThat(count("reservation")).isEqualTo(1);
        assertThat(offers.findById(offer).orElseThrow().quantity()).isZero();
      } else {
        assertThat(bookingStatus).isEqualTo(404);
        assertThat(deletionStatus).isEqualTo(200);
        assertThat(count("client")).isZero();
        assertThat(count("reservation")).isZero();
        assertThat(offers.findById(offer).orElseThrow().quantity()).isEqualTo(1);
      }
    } finally {
      start.countDown();
    }
  }

  @Test
  void callersCannotOverrideSnapshotsOrIdentifiersAndRemovedFieldsAreNotPersisted() throws Exception {
    String client = createClient();
    String offer = createOffer(2);
    JsonNode created = response(post("/reservations"), """
        {"id":"fake","clientId":"%s","items":[{
          "offerId":"%s","quantity":1,"lineId":"fake","status":"PAID",
          "participant":{"name":"Unstored person","email":"person@example.org"},
          "timestamp":"2000-01-01T00:00:00Z",
          "offerSnapshot":{"price":{"amount":"0.00","currency":"PLN"}}
        }]}
        """.formatted(client, offer), 200);
    assertThat(created.get("id").asString()).isNotEqualTo("fake");
    JsonNode line = created.get("items").get(0);
    assertThat(line.get("lineId").asString()).isNotEqualTo("fake");
    assertThat(line.has("status")).isFalse();
    assertThat(line.has("participant")).isFalse();
    assertThat(line.get("offerSnapshot").get("price").get("amount").asString()).isEqualTo("1.01");
    assertThat(response(get("/reservations/{id}", created.get("id").asString()), null, 200))
        .isEqualTo(created);
    String stored = jdbc.queryForObject(
        "SELECT offer_items_data FROM reservation WHERE id = ?",
        String.class, created.get("id").asString());
    assertThat(stored).doesNotContain("participant", "status", "Unstored person");
  }

  @Test
  void concurrentReservationsCannotSellTheLastTicketTwice() throws Exception {
    assertConcurrentStock(1, 8);
  }

  @Test
  void concurrentReservationsRespectLargerInventory() throws Exception {
    assertConcurrentStock(5, 12);
  }

  private void assertConcurrentStock(int inventory, int callers) throws Exception {
    String client = createClient();
    String offer = createOffer(inventory);
    String body = basket(client, item(offer, 1));
    CountDownLatch ready = new CountDownLatch(callers);
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(callers)) {
      List<Future<Integer>> results = new ArrayList<>();
      for (int i = 0; i < callers; i++) {
        results.add(executor.submit(() -> {
          ready.countDown();
          if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for concurrent start");
          }
          return mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON)
              .content(body)).andReturn().getResponse().getStatus();
        }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      List<Integer> statuses = new ArrayList<>();
      for (Future<Integer> result : results) {
        statuses.add(result.get(20, TimeUnit.SECONDS));
      }
      assertThat(statuses).containsOnly(200, 409);
      assertThat(statuses.stream().filter(code -> code == 200).count()).isEqualTo(inventory);
      assertThat(count("reservation")).isEqualTo(inventory);
      assertThat(offers.findById(offer).orElseThrow().quantity()).isZero();
    } finally {
      start.countDown();
    }
  }

  private String createClient() throws Exception {
    return response(post("/clients"), "{\"name\":\"Buyer\",\"email\":\"buyer@example.org\"}", 200)
        .get("id").asString();
  }

  private String createOffer(int quantity) throws Exception {
    return response(post("/offer/create"), """
        {"name":"Concert","vendor":{"name":"Vendor"},
         "price":{"amount":"1.005","currency":"PLN"},"quantity":%d}
        """.formatted(quantity), 200).get("id").asString();
  }

  private JsonNode response(MockHttpServletRequestBuilder request, String body, int expected)
      throws Exception {
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    var result = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse();
    return result.getContentAsString().isEmpty() ? null : mapper.readTree(result.getContentAsString());
  }

  private int count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
  }

  private static String basket(String clientId, String items) {
    return "{\"clientId\":\"" + clientId + "\",\"items\":[" + items + "]}";
  }

  private static String item(String offerId, int quantity) {
    return "{\"offerId\":\"" + offerId + "\",\"quantity\":" + quantity + "}";
  }
}
