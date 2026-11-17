package pl.edu.uj.student.wzorceprojektowe.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.edu.uj.student.wzorceprojektowe.shared.model.BlikPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.CardPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.CashPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;
import pl.edu.uj.student.wzorceprojektowe.shared.model.PaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.PaymentMethod;

@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:sqlite:build/payment-repository-test-${random.uuid}.db",
        "spring.datasource.hikari.connection-init-sql=PRAGMA foreign_keys = ON",
        "spring.datasource.hikari.data-source-properties.busy_timeout=10000",
        "spring.datasource.hikari.maximum-pool-size=8"
    })
class PaymentRepositoryTest {
  private static final String RESERVATION_ID = "reservation-1";
  private static final Instant TIMESTAMP = Instant.parse("2026-01-01T00:00:00.123Z");

  @Autowired
  private JdbcTemplate jdbc;
  @Autowired
  private PaymentRepository payments;

  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM payment");
    jdbc.update("DELETE FROM reservation");
    jdbc.update("DELETE FROM client");
    jdbc.update(
        "INSERT INTO client (id, name, email) VALUES ('client-1', 'Buyer', 'buyer@example.org')");
    jdbc.update(
        """
            INSERT INTO reservation
              (id, version, client_id, offer_items_data, status, create_timestamp,
               last_modified_timestamp)
            VALUES (?, 1, 'client-1', '[]', 'RESERVED', 0, 0)
            """,
        RESERVATION_ID);
  }

  @Test
  void cardPaymentRoundTripsThroughDatabase() {
    Payment payment =
        new Payment(
            "payment-1",
            RESERVATION_ID,
            "10.00",
            "PLN",
            new PaymentDetails(
                PaymentMethod.CARD,
                new CardPaymentDetails(
                    "4111111111111111", "123", 12, 2030, "Jan Kowalski", "ul. Przykładowa 1"),
                null,
                null),
            TIMESTAMP);

    assertThat(payments.insert(payment)).isEqualTo(payment);
    assertThat(payments.findById("payment-1")).contains(payment);

    assertThat(
            jdbc.queryForObject(
                "SELECT payment_method FROM payment WHERE id = 'payment-1'", String.class))
        .isEqualTo("CARD");
    // details column stores only the card details object, not the PaymentDetails wrapper
    String detailsJson =
        jdbc.queryForObject(
            "SELECT payment_method_details FROM payment WHERE id = 'payment-1'", String.class);
    assertThat(detailsJson).contains("4111111111111111").doesNotContain("cardPayment", "paymentMethod");
    assertThat(
            jdbc.queryForObject(
                "SELECT payment_timestamp FROM payment WHERE id = 'payment-1'", Long.class))
        .isEqualTo(TIMESTAMP.toEpochMilli());
  }

  @Test
  void cashAndBlikPaymentsRoundTripThroughDatabase() {
    Payment cash =
        new Payment(
            "payment-cash",
            RESERVATION_ID,
            "5.00",
            "PLN",
            new PaymentDetails(PaymentMethod.CASH, null, new CashPaymentDetails("AGENT-1"), null),
            TIMESTAMP);
    Payment blik =
        new Payment(
            "payment-blik",
            RESERVATION_ID,
            "7.50",
            "EUR",
            new PaymentDetails(
                PaymentMethod.BLIK, null, null, new BlikPaymentDetails("123456", "jan@example.org")),
            TIMESTAMP);

    payments.insert(cash);
    payments.insert(blik);

    assertThat(payments.findById("payment-cash")).contains(cash);
    assertThat(payments.findById("payment-blik")).contains(blik);
  }

  @Test
  void findByIdOfUnknownPaymentIsEmpty() {
    assertThat(payments.findById(UUID.randomUUID().toString())).isEmpty();
  }

  @Test
  void findByReservationIdReturnsPaymentsOrderedByTimestamp() {
    Payment later =
        new Payment(
            "payment-later",
            RESERVATION_ID,
            "5.00",
            "PLN",
            new PaymentDetails(PaymentMethod.CASH, null, new CashPaymentDetails("AGENT-1"), null),
            TIMESTAMP.plusSeconds(60));
    Payment earlier =
        new Payment(
            "payment-earlier",
            RESERVATION_ID,
            "5.00",
            "PLN",
            new PaymentDetails(PaymentMethod.CASH, null, new CashPaymentDetails("AGENT-2"), null),
            TIMESTAMP);

    payments.insert(later);
    payments.insert(earlier);

    assertThat(payments.findByReservationId(RESERVATION_ID))
        .containsExactly(earlier, later);
    assertThat(payments.findByReservationId("unknown-reservation")).isEmpty();
  }

  @Test
  void insertWithUnknownReservationViolatesForeignKey() {
    Payment orphan =
        new Payment(
            "payment-orphan",
            UUID.randomUUID().toString(),
            "10.00",
            "PLN",
            new PaymentDetails(PaymentMethod.CASH, null, new CashPaymentDetails("AGENT-1"), null),
            TIMESTAMP);

    assertThatThrownBy(() -> payments.insert(orphan)).isInstanceOf(DataAccessException.class);
  }

  @Test
  void paymentsInsertedDirectlyAreMappedBack() {
    jdbc.update(
        """
            INSERT INTO payment
              (id, reservation_id, amount, currency, payment_method, payment_method_details,
               payment_timestamp)
            VALUES ('payment-sql', ?, '3.25', 'PLN', 'BLIK', '{"blikCode":"654321","email":"a@b.org"}', ?)
            """,
        RESERVATION_ID,
        TIMESTAMP.toEpochMilli());

    Payment expected =
        new Payment(
            "payment-sql",
            RESERVATION_ID,
            "3.25",
            "PLN",
            new PaymentDetails(
                PaymentMethod.BLIK, null, null, new BlikPaymentDetails("654321", "a@b.org")),
            TIMESTAMP);
    List<Payment> found = payments.findByReservationId(RESERVATION_ID);
    assertThat(found).containsExactly(expected);
  }
}
