package pl.edu.uj.student.wzorceprojektowe.offer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Vendor;
import pl.edu.uj.student.wzorceprojektowe.offer.repository.OfferRepository;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite::memory:",
    "spring.datasource.hikari.maximum-pool-size=1",
    "spring.datasource.hikari.minimum-idle=1",
    "spring.datasource.hikari.max-lifetime=0",
    "spring.sql.init.mode=always"
})
@Transactional
class OfferPersistenceRegressionTest {

  @Autowired private OfferRepository repository;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private WebApplicationContext context;

  private final JsonMapper mapper = JsonMapper.builder().build();
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  void staleUpdateThrowsAndPreservesTheSuccessfulWrite() {
    Offer created = insertOffer();
    Offer firstRead = repository.findById(created.id()).orElseThrow();
    Offer secondRead = repository.findById(created.id()).orElseThrow();

    Offer saved = repository.update(withQuantity(firstRead, 8));

    assertThat(saved.version()).isEqualTo(firstRead.version() + 1);

    assertThatThrownBy(
            () -> repository.update(withQuantity(secondRead, 3)))
        .isInstanceOf(OptimisticLockingFailureException.class);

    Offer stored = repository.findById(created.id()).orElseThrow();
    assertThat(stored).isEqualTo(saved);
    assertThat(stored.quantity()).isEqualTo(8);
    assertThat(stored.version()).isEqualTo(firstRead.version() + 1);
  }

  @Test
  void updateAfterDeletionThrowsAndDoesNotRecreateTheOffer() {
    Offer created = insertOffer();
    Offer previouslyRead = repository.findById(created.id()).orElseThrow();

    assertThat(repository.deleteById(created.id())).isPresent();

    assertThatThrownBy(
            () -> repository.update(withQuantity(previouslyRead, 3)))
        .isInstanceOf(OptimisticLockingFailureException.class);

    assertThat(repository.findById(created.id())).isEmpty();
  }

  @Test
  void createAndGetReturnTheSameNormalizedPrice() throws Exception {
    String payload = """
        {
          "name": "Rounding regression",
          "vendor": {"name": "Rounding vendor"},
          "price": {"amount": "1.005", "currency": "PLN"},
          "description": "Persistence round trip",
          "quantity": 1
        }
        """;

    String creationBody = mockMvc.perform(
            post("/offer/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode created = mapper.readTree(creationBody);
    String id = created.get("id").asString();

    assertThat(created.get("price").get("amount").asString())
        .isEqualTo("1.01");
    assertThat(created.get("price").get("currency").asString())
        .isEqualTo("PLN");

    // Verify the actual SQL representation, not just serialized output.
    assertThat(jdbc.queryForObject(
            "SELECT amount FROM offer WHERE id = ?",
            String.class,
            id))
        .isEqualTo("1.01");

    Offer stored = repository.findById(id).orElseThrow();
    assertThat(stored.price()).isEqualTo(new Money("PLN", "1.01"));

    String fetchedBody = mockMvc.perform(get("/offer/{id}", id))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    assertThat(mapper.readTree(fetchedBody)).isEqualTo(created);
  }

  private Offer insertOffer() {
    String vendorName = "Persistence regression vendor";
    jdbc.update("INSERT INTO vendor(name) VALUES (?)", vendorName);

    return repository.insert(new Offer(
        "",
        "Regression offer",
        new Vendor(vendorName),
        new Money("PLN", "10.00"),
        "Test description",
        10,
        null));
  }

  private static Offer withQuantity(Offer original, int quantity) {
    return new Offer(
        original.id(),
        original.name(),
        original.vendor(),
        original.price(),
        original.description(),
        quantity,
        original.version());
  }
}
