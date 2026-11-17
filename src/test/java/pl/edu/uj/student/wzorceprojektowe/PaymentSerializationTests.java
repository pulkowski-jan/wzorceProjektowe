package pl.edu.uj.student.wzorceprojektowe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import pl.edu.uj.student.wzorceprojektowe.shared.model.BlikPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.CardPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.CashPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;
import pl.edu.uj.student.wzorceprojektowe.shared.model.PaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.PaymentMethod;
import tools.jackson.databind.json.JsonMapper;

class PaymentSerializationTests {
  private final JsonMapper mapper = JsonMapper.builder().build();

  @Test
  void cardPaymentRoundTripsWithOnlyCardDetail() {
    var payment =
        new Payment(
            "payment-1",
            "reservation-1",
            "10.00",
            "PLN",
            new PaymentDetails(
                PaymentMethod.CARD,
                new CardPaymentDetails(
                    "4111111111111111", "123", 12, 2030, "Jan Kowalski", "ul. Przykładowa 1"),
                null,
                null),
            Instant.parse("2026-01-01T00:00:00Z"));

    var json = mapper.readTree(mapper.writeValueAsString(payment));
    assertTrue(json.get("amount").isString());
    assertEquals("10.00", json.get("amount").asString());
    assertEquals("PLN", json.get("currency").asString());
    assertEquals("CARD", json.get("paymentDetails").get("paymentMethod").asString());
    assertTrue(json.get("paymentDetails").has("cardPayment"));
    assertTrue(json.get("paymentDetails").get("cashPayment").isNull());
    assertTrue(json.get("paymentDetails").get("blikPayment").isNull());
    assertEquals(
        "ul. Przykładowa 1",
        json.get("paymentDetails").get("cardPayment").get("billingAddress").asString());

    var copy = mapper.readValue(mapper.writeValueAsString(payment), Payment.class);
    assertEquals(payment, copy);
  }

  @Test
  void cashPaymentRoundTripsWithOnlyCashDetail() {
    var payment =
        new Payment(
            "payment-1",
            "reservation-1",
            "10.00",
            "PLN",
            new PaymentDetails(PaymentMethod.CASH, null, new CashPaymentDetails("AGENT-1"), null),
            Instant.parse("2026-01-01T00:00:00Z"));

    var json = mapper.readTree(mapper.writeValueAsString(payment));
    assertTrue(json.get("paymentDetails").has("cashPayment"));
    assertTrue(json.get("paymentDetails").get("cardPayment").isNull());
    assertTrue(json.get("paymentDetails").get("blikPayment").isNull());

    var copy = mapper.readValue(mapper.writeValueAsString(payment), Payment.class);
    assertEquals(payment, copy);
  }

  @Test
  void blikPaymentRoundTripsWithOnlyBlikDetail() {
    var payment =
        new Payment(
            "payment-1",
            "reservation-1",
            "10.00",
            "PLN",
            new PaymentDetails(
                PaymentMethod.BLIK, null, null, new BlikPaymentDetails("123456", "jan@example.org")),
            Instant.parse("2026-01-01T00:00:00Z"));

    var json = mapper.readTree(mapper.writeValueAsString(payment));
    assertTrue(json.get("paymentDetails").has("blikPayment"));
    assertTrue(json.get("paymentDetails").get("cardPayment").isNull());
    assertTrue(json.get("paymentDetails").get("cashPayment").isNull());

    var copy = mapper.readValue(mapper.writeValueAsString(payment), Payment.class);
    assertEquals(payment, copy);
  }
}
