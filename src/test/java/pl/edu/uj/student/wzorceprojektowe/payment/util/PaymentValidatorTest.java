package pl.edu.uj.student.wzorceprojektowe.payment.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.BlikPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.CardPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.CashPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;
import pl.edu.uj.student.wzorceprojektowe.shared.model.PaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.PaymentMethod;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.PolicyExecutor;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy;

class PaymentValidatorTest {

  private static final Instant TIMESTAMP = Instant.parse("2026-01-01T00:00:00Z");
  private static final CardPaymentDetails VALID_CARD =
      new CardPaymentDetails("4111111111111111", "123", 12, 2030, "Jan Kowalski", null);
  private static final CashPaymentDetails VALID_CASH = new CashPaymentDetails("AGENT-1");
  private static final BlikPaymentDetails VALID_BLIK =
      new BlikPaymentDetails("123456", "jan@example.org");

  private final PaymentValidator underTest = new PaymentValidator(new PolicyExecutor<>());

  private static Payment cardPayment(CardPaymentDetails card) {
    return new Payment(
        "payment-1",
        "reservation-1",
        "10.00",
        "PLN",
        new PaymentDetails(PaymentMethod.CARD, card, null, null),
        TIMESTAMP);
  }

  private static Payment cashPayment(CashPaymentDetails cash) {
    return new Payment(
        "payment-1",
        "reservation-1",
        "10.00",
        "PLN",
        new PaymentDetails(PaymentMethod.CASH, null, cash, null),
        TIMESTAMP);
  }

  private static Payment blikPayment(BlikPaymentDetails blik) {
    return new Payment(
        "payment-1",
        "reservation-1",
        "10.00",
        "PLN",
        new PaymentDetails(PaymentMethod.BLIK, null, null, blik),
        TIMESTAMP);
  }

  @Test
  void validPaymentsPassForEveryMethod() {
    assertDoesNotThrow(() -> underTest.validatePayment(cardPayment(VALID_CARD)));
    assertDoesNotThrow(() -> underTest.validatePayment(cashPayment(VALID_CASH)));
    assertDoesNotThrow(() -> underTest.validatePayment(blikPayment(VALID_BLIK)));
  }

  @Test
  void cardPaymentWithBillingAddressPasses() {
    CardPaymentDetails card =
        new CardPaymentDetails(
            "4111111111111111", "1234", 1, 2031, "Jan Kowalski", "ul. Przykładowa 1");
    assertDoesNotThrow(() -> underTest.validatePayment(cardPayment(card)));
  }

  @Test
  void nullPaymentThrowsInsteadOfNpe() {
    RequestValidationException ex =
        assertThrows(RequestValidationException.class, () -> underTest.validatePayment(null));
    assertTrue(ex.getMessage().contains("null"), ex.getMessage());
  }

  @Test
  void invalidCommonFieldsThrow() {
    assertThrows(
        RequestValidationException.class,
        () ->
            underTest.validatePayment(
                new Payment(
                    "payment-1",
                    " ",
                    "10.00",
                    "PLN",
                    new PaymentDetails(PaymentMethod.CASH, null, VALID_CASH, null),
                    TIMESTAMP)));
    assertThrows(
        RequestValidationException.class,
        () ->
            underTest.validatePayment(
                new Payment(
                    "payment-1",
                    "reservation-1",
                    "-1.00",
                    "PLN",
                    new PaymentDetails(PaymentMethod.CASH, null, VALID_CASH, null),
                    TIMESTAMP)));
    assertThrows(
        RequestValidationException.class,
        () ->
            underTest.validatePayment(
                new Payment(
                    "payment-1",
                    "reservation-1",
                    "not-a-number",
                    "PLN",
                    new PaymentDetails(PaymentMethod.CASH, null, VALID_CASH, null),
                    TIMESTAMP)));
    assertThrows(
        RequestValidationException.class,
        () ->
            underTest.validatePayment(
                new Payment(
                    "payment-1",
                    "reservation-1",
                    "10.00",
                    "",
                    new PaymentDetails(PaymentMethod.CASH, null, VALID_CASH, null),
                    TIMESTAMP)));
    assertThrows(
        RequestValidationException.class,
        () ->
            underTest.validatePayment(
                new Payment("payment-1", "reservation-1", "10.00", "PLN", null, TIMESTAMP)));
    assertThrows(
        RequestValidationException.class,
        () ->
            underTest.validatePayment(
                new Payment(
                    "payment-1",
                    "reservation-1",
                    "10.00",
                    "PLN",
                    new PaymentDetails(PaymentMethod.CASH, null, VALID_CASH, null),
                    null)));
  }

  @Test
  void zeroAmountIsAllowed() {
    Payment payment =
        new Payment(
            "payment-1",
            "reservation-1",
            "0.00",
            "PLN",
            new PaymentDetails(PaymentMethod.CASH, null, VALID_CASH, null),
            TIMESTAMP);
    assertDoesNotThrow(() -> underTest.validatePayment(payment));
  }

  @Test
  void missingOrMismatchedDetailsThrow() {
    // no method
    assertThrows(
        RequestValidationException.class,
        () ->
            underTest.validatePayment(
                new Payment(
                    "payment-1",
                    "reservation-1",
                    "10.00",
                    "PLN",
                    new PaymentDetails(null, VALID_CARD, null, null),
                    TIMESTAMP)));
    // no detail at all
    assertThrows(
        RequestValidationException.class,
        () ->
            underTest.validatePayment(
                new Payment(
                    "payment-1",
                    "reservation-1",
                    "10.00",
                    "PLN",
                    new PaymentDetails(PaymentMethod.CARD, null, null, null),
                    TIMESTAMP)));
    // two details
    assertThrows(
        RequestValidationException.class,
        () ->
            underTest.validatePayment(
                new Payment(
                    "payment-1",
                    "reservation-1",
                    "10.00",
                    "PLN",
                    new PaymentDetails(PaymentMethod.CARD, VALID_CARD, VALID_CASH, null),
                    TIMESTAMP)));
    // detail does not match the method
    assertThrows(
        RequestValidationException.class,
        () ->
            underTest.validatePayment(
                new Payment(
                    "payment-1",
                    "reservation-1",
                    "10.00",
                    "PLN",
                    new PaymentDetails(PaymentMethod.CARD, null, VALID_CASH, null),
                    TIMESTAMP)));
  }

  @Test
  void invalidCardDetailsThrow() {
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(cardPayment(new CardPaymentDetails(" ", "123", 12, 2030, "Jan Kowalski", null))));
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(cardPayment(new CardPaymentDetails("4111111111111111", "12", 12, 2030, "Jan Kowalski", null))));
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(cardPayment(new CardPaymentDetails("4111111111111111", "12345", 12, 2030, "Jan Kowalski", null))));
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(cardPayment(new CardPaymentDetails("4111111111111111", "12a", 12, 2030, "Jan Kowalski", null))));
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(cardPayment(new CardPaymentDetails("4111111111111111", "123", 0, 2030, "Jan Kowalski", null))));
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(cardPayment(new CardPaymentDetails("4111111111111111", "123", 13, 2030, "Jan Kowalski", null))));
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(cardPayment(new CardPaymentDetails("4111111111111111", "123", null, 2030, "Jan Kowalski", null))));
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(cardPayment(new CardPaymentDetails("4111111111111111", "123", 12, 1900, "Jan Kowalski", null))));
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(cardPayment(new CardPaymentDetails("4111111111111111", "123", 12, 2030, " ", null))));
  }

  @Test
  void invalidCashDetailsThrow() {
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(cashPayment(new CashPaymentDetails(" "))));
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(cashPayment(new CashPaymentDetails(null))));
  }

  @Test
  void invalidBlikDetailsThrow() {
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(blikPayment(new BlikPaymentDetails("12345", "jan@example.org"))));
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(blikPayment(new BlikPaymentDetails("1234567", "jan@example.org"))));
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(blikPayment(new BlikPaymentDetails("12345a", "jan@example.org"))));
    assertThrows(
        RequestValidationException.class,
        () -> underTest.validatePayment(blikPayment(new BlikPaymentDetails("123456", "not-an-email"))));
  }

  @Test
  void additionalPolicyIsEvaluated() {
    List<ValidationPolicy<Payment>> extra =
        List.of(new ValidationPolicy<>(payment -> payment.id() != null, "Payment must have id"));
    Payment withoutId =
        new Payment(
            null,
            "reservation-1",
            "10.00",
            "PLN",
            new PaymentDetails(PaymentMethod.CASH, null, VALID_CASH, null),
            TIMESTAMP);
    assertThrows(
        RequestValidationException.class, () -> underTest.validatePayment(withoutId, extra));
  }
}
