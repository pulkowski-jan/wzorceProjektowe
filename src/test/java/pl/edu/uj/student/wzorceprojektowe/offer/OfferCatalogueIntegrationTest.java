package pl.edu.uj.student.wzorceprojektowe.offer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Integration tests for the offer catalogue ({@code /offer}).
 *
 * <p>Tests drive the real offer controller through {@link MockMvc}, which exercises the full stack:
 * HTTP layer, controller exception mapping, services, repositories and the SQLite database.
 */
@Slf4j
@SpringBootTest
class OfferCatalogueIntegrationTest {
  @Autowired private WebApplicationContext context;
  private final JsonMapper mapper = JsonMapper.builder().build();
  private MockMvc mockMvc;
  List<String> createdIds;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    createdIds = new ArrayList<>();
  }

  @AfterEach
  void tearDown() {
    createdIds.forEach(id -> delete("/offer/{id}", id));
    createdIds.clear();
  }

  // ---------- GET /offer/{id} ----------

  @Nested
  class GetById {

    @Test
    void returnsOfferCreatedEarlier() {
      String id =
          createOffer(
              """
              {
                "name": "Koncert: Iron Maiden",
                "vendor": {"name": "Ticketmaster"},
                "price": {"amount": "350.00", "currency": "PLN"},
                "description": "Bilet na koncert",
                "quantity": 1
              }
              """);
      HttpResponse response = get("/offer/{id}", id);
      assertSuccess(response);
      assertOffer(
          readTree(response.body()),
          id,
          "Koncert: Iron Maiden",
          "Ticketmaster",
          "350.00",
          "PLN",
          "Bilet na koncert",
          1);
    }

    @Test
    void unknownIdReturnsNotFound() {
      assertThat(get("/offer/{id}", UUID.randomUUID()).status())
          .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void malformedIdReturnsNotFound() {
      assertThat(get("/offer/{id}", "not-a-uuid").status()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }
  }

  // ---------- GET /offer/search ----------
  @Nested
  class Search {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("searchCases")
    void searchReturnsExactlyTheExpectedOffers(SearchCase testCase) {
      for (String payload : testCase.payloads()) {
        createOffer(payload);
      }
      HttpResponse response =
          getWithParams("/offer/search", testCase.queryParams().toArray(String[]::new));
      assertSuccess(response);
      log.debug(response.body());
      JsonNode results = readTree(response.body());
      assertThat(results.isArray()).isTrue();

      List<String> expectedIds =
          testCase.expectedIdIndexes().stream().map(createdIds::get).toList();
      List<String> actualIds =
          results.valueStream().map(node -> node.get("id").asString()).toList();

      assertThat(actualIds)
          .as("Search %s should return exactly offers %s".formatted(testCase, expectedIds))
          .containsExactlyInAnyOrderElementsOf(expectedIds);
    }

    @Test
    void requiresAtLeastOneSearchParameter() {
      assertThat(getWithParams("/offer/search").status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    record SearchCase(
        String description,
        List<String> payloads,
        List<String> queryParams,
        List<Integer> expectedIdIndexes) {}

    static Stream<SearchCase> searchCases() {
      return Stream.of(
          new SearchCase(
              "finds offer by name",
              List.of(
                  offer(
                      "Spektakl: Hamlet", "Teatr Stary", "120.00", "PLN", "Bilet na spektakl", 1)),
              List.of("name", "Hamlet"),
              List.of(0)),
          new SearchCase(
              "finds offer by vendor",
              List.of(offer("Koncert: Tina Turner", "LiveNation", "299.50", "EUR", "Bilet VIP", 3)),
              List.of("vendor", "LiveNation"),
              List.of(0)),
          new SearchCase(
              "finds offer by name and vendor",
              List.of(
                  offer("Spektakl: Dziady", "Teatr Stary", "50.00", "PLN", "Bilet na spektakl", 2)),
              List.of("name", "Dziady", "vendor", "Teatr Stary"),
              List.of(0)),
          new SearchCase(
              "excludes offers with zero quantity",
              List.of(offer("Wyprzedaz", "Outlet", "0.00", "PLN", "Nic nie zostalo", 0)),
              List.of("name", "Wyprzedaz"),
              List.of()),
          new SearchCase(
              "returns empty list when nothing matches",
              List.of(),
              List.of("name", "NieistniejacaOferta"),
              List.of()),
          new SearchCase(
              "does not return offer when only one search param matches",
              List.of(
                  offer("Dziady", "Teatr", "50.00", "PLN", "Bilet na spektakl", 2),
                  offer("Spektakl", "Teatr", "50.00", "PLN", "Bilet na spektakl", 2),
                  offer("Dziady", "Kino", "50.00", "PLN", "Bilet na spektakl", 2)),
              List.of("name", "Dziady", "vendor", "Teatr"),
              List.of(0)),
          new SearchCase(
              "searches case-insensitively",
              List.of(
                  offer("dziady", "teatr", "50.00", "PLN", "Bilet na spektakl", 2),
                  offer("DZIADY", "TEATR", "50.00", "PLN", "Bilet na spektakl", 2)),
              List.of("name", "Dziady", "vendor", "Teatr"),
              List.of(0, 1)),
          new SearchCase(
              "searches by partial name",
              List.of(
                  offer("Dziady II", "teatr", "50.00", "PLN", "Bilet na spektakl", 2),
                  offer("Dziady III", "TEATR", "50.00", "PLN", "Bilet na spektakl", 2)),
              List.of("name", "Dziady", "vendor", "Teatr"),
              List.of(0, 1)),
          new SearchCase(
              "searches by partial vendor",
              List.of(
                  offer("Dziady", "Teatr Nowy", "50.00", "PLN", "Bilet na spektakl", 2),
                  offer("Dziady", "Teatr Stary", "50.00", "PLN", "Bilet na spektakl", 2)),
              List.of("name", "Dziady", "vendor", "Teatr"),
              List.of(0, 1)),
          new SearchCase(
              "searches by partial name and vendor",
              List.of(
                  offer("Dziady II", "Teatr Nowy", "50.00", "PLN", "Bilet na spektakl", 2),
                  offer("Dziady III", "Teatr Stary", "50.00", "PLN", "Bilet na spektakl", 2)),
              List.of("name", "Dziady", "vendor", "Teatr"),
              List.of(0, 1)));
    }
  }

  // ---------- POST /offer/create ----------

  @Nested
  class Create {

    @Test
    void createsOfferWithGeneratedId() {
      String id =
          createOffer(
              """
              {
                "name": "Koncert: Beyonce",
                "vendor": {"name": "LiveNation"},
                "price": {"amount": "299.50", "currency": "EUR"},
                "description": "Bilet VIP",
                "quantity": 3
              }
              """);
      HttpResponse response = get("/offer/{id}", id);
      assertSuccess(response);
      assertOffer(
          readTree(response.body()),
          id,
          "Koncert: Beyonce",
          "LiveNation",
          "299.50",
          "EUR",
          "Bilet VIP",
          3);
    }

    @Test
    void providedIdIsIgnored() {
      HttpResponse response =
          post(
              "/offer/create",
              """
              {
                "id": "00000000-0000-0000-0000-000000000000",
                "name": "Ignored Id",
                "vendor": {"name": "Vendor"},
                "price": {"amount": "1.00", "currency": "PLN"},
                "quantity": 1
              }
              """);
      assertSuccess(response);
      JsonNode offer = readTree(response.body());
      String generatedId = offer.get("id").asString();
      assertThat(generatedId).isNotEqualTo("00000000-0000-0000-0000-000000000000");
    }

    @Test
    void createsVendorWhenAbsent() {
      String id =
          createOffer(
              """
              {
                "name": "Nowa Oferta",
                "vendor": {"name": "Nowy Sprzedawca"},
                "price": {"amount": "10.00", "currency": "PLN"},
                "quantity": 5
              }
              """);
      assertThat(id).isNotBlank();
    }

    @Test
    void allowsEmptyDescription() {
      String id =
          createOffer(
              """
              {
                "name": "Bez Opisu",
                "vendor": {"name": "Vendor"},
                "price": {"amount": "7.50", "currency": "PLN"},
                "description": "",
                "quantity": 5
              }
              """);
      HttpResponse response = get("/offer/{id}", id);
      assertSuccess(response);
      assertThat(readTree(response.body()).get("description").asString()).isEqualTo("");
    }

    @Test
    void rejectsMissingVendor() {
      assertThat(
              post(
                      "/offer/create",
                      """
                      {
                        "name": "X",
                        "vendor": null,
                        "price": {"amount": "1.00", "currency": "PLN"},
                        "quantity": 1
                      }
                      """)
                  .status())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void rejectsBlankVendorName() {
      assertThat(
              post(
                      "/offer/create",
                      """
                      {
                        "name": "X",
                        "vendor": {"name": ""},
                        "price": {"amount": "1.00", "currency": "PLN"},
                        "quantity": 1
                      }
                      """)
                  .status())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void rejectsBlankName() {
      assertThat(
              post(
                      "/offer/create",
                      """
                      {
                        "name": "",
                        "vendor": {"name": "Vendor"},
                        "price": {"amount": "1.00", "currency": "PLN"},
                        "quantity": 1
                      }
                      """)
                  .status())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void rejectsMissingName() {
      assertThat(
              post(
                      "/offer/create",
                      """
                      {
                        "vendor": {"name": "Vendor"},
                        "price": {"amount": "1.00", "currency": "PLN"},
                        "quantity": 1
                      }
                      """)
                  .status())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void rejectsMissingPrice() {
      assertThat(
              post(
                      "/offer/create",
                      """
                      {
                        "name": "X",
                        "vendor": {"name": "Vendor"},
                        "quantity": 1
                      }
                      """)
                  .status())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void rejectsBlankCurrency() {
      assertThat(
              post(
                      "/offer/create",
                      """
                      {
                        "name": "X",
                        "vendor": {"name": "Vendor"},
                        "price": {"amount": "1.00", "currency": ""},
                        "quantity": 1
                      }
                      """)
                  .status())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void rejectsNegativePrice() {
      assertThat(
              post(
                      "/offer/create",
                      """
                      {
                        "name": "X",
                        "vendor": {"name": "Vendor"},
                        "price": {"amount": "-5.00", "currency": "PLN"},
                        "quantity": 1
                      }
                      """)
                  .status())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void rejectsNegativeQuantity() {
      assertThat(
              post(
                      "/offer/create",
                      """
                      {
                        "name": "X",
                        "vendor": {"name": "Vendor"},
                        "price": {"amount": "1.00", "currency": "PLN"},
                        "quantity": -1
                      }
                      """)
                  .status())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }
  }

  // ---------- POST /offer/update ----------

  @Nested
  class Update {

    @Test
    void updatesProvidedFields() {
      String id =
          createOffer(
              """
              {
                "name": "Koncert",
                "vendor": {"name": "Ticketmaster"},
                "price": {"amount": "100.00", "currency": "PLN"},
                "description": "Stary opis",
                "quantity": 5
              }
              """);
      String update =
          """
          {
            "id": "%s",
            "name": "Koncert: Edytowany",
            "vendor": {"name": "Ticketmaster"},
            "price": {"amount": "200.00", "currency": "EUR"},
            "description": "Nowy opis",
            "quantity": 2
          }
          """
              .formatted(id);
      HttpResponse updateResponse = post("/offer/update", update);
      assertSuccess(updateResponse);
      assertOffer(
          readTree(updateResponse.body()),
          id,
          "Koncert: Edytowany",
          "Ticketmaster",
          "200.00",
          "EUR",
          "Nowy opis",
          2);
      HttpResponse getResponse = get("/offer/{id}", id);
      assertThat(getResponse.body()).isEqualTo(updateResponse.body());
    }

    @Test
    void cannotChangeVendor() {
      String id =
          createOffer(
              """
              {
                "name": "Koncert",
                "vendor": {"name": "Ticketmaster"},
                "price": {"amount": "100.00", "currency": "PLN"},
                "quantity": 5
              }
              """);
      String update =
          """
          {
            "id": "%s",
            "name": "Koncert",
            "vendor": {"name": "Inny Sprzedawca"},
            "price": {"amount": "100.00", "currency": "PLN"},
            "quantity": 5
          }
          """
              .formatted(id);
      assertThat(post("/offer/update", update).status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void updatePreservesExistingFieldValues() {
      // Only id and name are sent; vendor/price/description/quantity are absent and must be kept.
      String id =
          createOffer(
              """
              {
                "name": "Koncert",
                "vendor": {"name": "Ticketmaster"},
                "price": {"amount": "100.00", "currency": "PLN"},
                "description": "Opis",
                "quantity": 5
              }
              """);
      String update =
          """
          {
            "id": "%s",
            "name": "Tylko Nowa Nazwa"
          }
          """
              .formatted(id);
      HttpResponse response = post("/offer/update", update);
      assertSuccess(response);
      JsonNode offer = readTree(response.body());
      assertThat(offer.get("id").asString()).isEqualTo(id);
      assertThat(offer.get("name").asString()).isEqualTo("Tylko Nowa Nazwa");
      assertThat(offer.get("vendor").get("name").asString()).isEqualTo("Ticketmaster");
      assertThat(offer.get("price").get("amount").asString()).isEqualTo("100.00");
      assertThat(offer.get("price").get("currency").asString()).isEqualTo("PLN");
      assertThat(offer.get("description").asString()).isEqualTo("Opis");
      assertThat(offer.get("quantity").asInt()).isEqualTo(5);
    }

    @Test
    void unknownIdReturnsNotFound() {
      assertThat(
              post(
                      "/offer/update",
                      """
                      {
                        "id": "%s",
                        "name": "X",
                        "vendor": {"name": "Vendor"},
                        "price": {"amount": "1.00", "currency": "PLN"},
                        "quantity": 1
                      }
                      """
                          .formatted(UUID.randomUUID()))
                  .status())
          .isEqualTo(HttpStatus.NOT_FOUND.value());
    }
  }

  // ---------- DELETE /offer/{id} ----------

  @Nested
  class Delete {

    @Test
    void deletesOfferAndReturnsIt() {
      String id =
          createOffer(
              """
              {
                "name": "Do Usuniecia",
                "vendor": {"name": "Vendor"},
                "price": {"amount": "5.00", "currency": "PLN"},
                "description": "Do usuniecia",
                "quantity": 1
              }
              """);
      HttpResponse response = delete("/offer/{id}", id);
      assertSuccess(response);
      assertOffer(
          readTree(response.body()),
          id,
          "Do Usuniecia",
          "Vendor",
          "5.00",
          "PLN",
          "Do usuniecia",
          1);

      assertThat(get("/offer/{id}", id).status()).isEqualTo(HttpStatus.NOT_FOUND.value());

      HttpResponse afterDelete = getWithParams("/offer/search", "name", "Do Usuniecia");
      assertThat(readTree(afterDelete.body()).isArray()).isTrue();
      assertThat(readTree(afterDelete.body()).size()).isEqualTo(0);
    }

    @Test
    void unknownIdReturnsNotFound() {
      assertThat(delete("/offer/{id}", UUID.randomUUID()).status())
          .isEqualTo(HttpStatus.NOT_FOUND.value());
    }
  }

  @Nested
  class MoneyDeserialization {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidPricePayloads")
    void rejectsInvalidPrice(String description, String priceJson) {
      String payload =
          """
          {
            "name": "Test",
            "vendor": {"name": "Teatr"},
            "price": %s,
            "description": "Opis",
            "quantity": 1
          }
          """
              .formatted(priceJson);

      HttpResponse response = post("/offer/create", payload);
      assertThat(response.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(response.body()).containsIgnoringCase("price");
    }

    static Stream<Arguments> invalidPricePayloads() {
      return Stream.of(
          Arguments.of("price is null", "null"),
          Arguments.of("price is a string", "\"120.00\""),
          Arguments.of("price is a number", "123.00"),
          Arguments.of("price is an array", "[]"),
          Arguments.of("price is an empty object", "{}"),
          Arguments.of("missing amount", "{\"currency\": \"PLN\"}"),
          Arguments.of("missing currency", "{\"amount\": \"120.00\"}"),
          Arguments.of("null amount", "{\"amount\": null, \"currency\": \"PLN\"}"),
          Arguments.of("numeric amount", "{\"amount\": 120, \"currency\": \"PLN\"}"),
          Arguments.of("non-numeric amount", "{\"amount\": \"abc\", \"currency\": \"PLN\"}"),
          Arguments.of("empty amount", "{\"amount\": \"\", \"currency\": \"PLN\"}"),
          Arguments.of("blank currency", "{\"amount\": \"120.00\", \"currency\": \"\"}"),
          Arguments.of("numeric currency", "{\"amount\": \"120.00\", \"currency\": 5}"));
    }
  }

  // ---------- helpers ----------

  private record HttpResponse(int status, String body) {}

  @SneakyThrows
  private HttpResponse execute(RequestBuilder request) {
    MvcResult result = mockMvc.perform(request).andReturn();
    return new HttpResponse(
        result.getResponse().getStatus(), result.getResponse().getContentAsString());
  }

  private HttpResponse get(String uri, Object... uriVariables) {
    return execute(MockMvcRequestBuilders.get(uri, uriVariables));
  }

  private HttpResponse getWithParams(String uri, String... nameValuePairs) {
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder =
        MockMvcRequestBuilders.get(uri);
    for (int i = 0; i + 1 < nameValuePairs.length; i += 2) {
      builder = builder.param(nameValuePairs[i], nameValuePairs[i + 1]);
    }
    return execute(builder);
  }

  private HttpResponse post(String uri, String body) {
    return execute(
        MockMvcRequestBuilders.post(uri).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private HttpResponse delete(String uri, Object... uriVariables) {
    return execute(MockMvcRequestBuilders.delete(uri, uriVariables));
  }

  private JsonNode readTree(String json) {
    try {
      return mapper.readTree(json);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot parse response JSON: %s".formatted(json), e);
    }
  }

  /** Creates an offer via the API and returns the id generated by the server. */
  @SneakyThrows
  String createOffer(String body) {
    HttpResponse response = post("/offer/create", body);
    assertSuccess(response);
    JsonNode offer = readTree(response.body());
    String id = offer.get("id").asString();
    createdIds.add(id);
    return id;
  }

  private void assertOffer(
      JsonNode offer,
      String id,
      String name,
      String vendor,
      String amount,
      String currency,
      String description,
      int quantity) {
    if (id != null) {
      assertThat(offer.get("id").asString()).isEqualTo(id);
    }
    assertThat(offer.get("name").asString()).isEqualTo(name);
    assertThat(offer.get("vendor").get("name").asString()).isEqualTo(vendor);
    assertThat(offer.get("price").get("amount").asString()).isEqualTo(amount);
    assertThat(offer.get("price").get("currency").asString()).isEqualTo(currency);
    assertThat(offer.get("description").asString()).isEqualTo(description);
    assertThat(offer.get("quantity").asInt()).isEqualTo(quantity);
  }

  private static String offer(
      String name,
      String vendor,
      String amount,
      String currency,
      String description,
      int quantity) {
    return """
    {
      "name": "%s",
      "vendor": {"name": "%s"},
      "price": {"amount": "%s", "currency": "%s"},
      "description": "%s",
      "quantity": %d
    }
    """
        .formatted(name, vendor, amount, currency, description, quantity);
  }

  private static void assertSuccess(HttpResponse response) {
    assertThat(response.status()).isEqualTo(HttpStatus.OK.value());
  }
}
