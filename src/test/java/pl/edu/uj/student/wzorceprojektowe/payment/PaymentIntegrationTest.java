package pl.edu.uj.student.wzorceprojektowe.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.edu.uj.student.wzorceprojektowe.payment.controller.PaymentController;
import pl.edu.uj.student.wzorceprojektowe.payment.model.PaymentResult;
import pl.edu.uj.student.wzorceprojektowe.payment.repository.PaymentRepository;
import pl.edu.uj.student.wzorceprojektowe.payment.service.PaymentFinalizer;
import pl.edu.uj.student.wzorceprojektowe.reservation.repository.ReservationRepository;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestConflictException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:sqlite:build/payment-integration-test-${random.uuid}.db",
    "spring.datasource.hikari.connection-init-sql=PRAGMA foreign_keys = ON",
    "spring.datasource.hikari.maximum-pool-size=1"
})
class PaymentIntegrationTest {
  private static final String RESERVATION_ID = "payment-integration-reservation";
  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00.123Z");
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PaymentController controller;
  @Autowired private PaymentFinalizer finalizer;
  @Autowired private PaymentRepository payments;
  @Autowired private ReservationRepository reservations;

  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM payment");
    jdbc.update("DELETE FROM reservation");
    jdbc.update("DELETE FROM client");
    jdbc.update("INSERT INTO client (id, name, email) VALUES ('buyer', 'Buyer', 'buyer@example.org')");
    assertThat(reservations.insertForClient(RESERVATION_ID, "buyer", NOW)).isTrue();
    Offer offer = new Offer("offer-1", "Test offer", null, new Money("PLN", "10.00"), "Test", 3, 1);
    reservations.storeItems(RESERVATION_ID,
        List.of(new OrderItem("line-1", offer, 3, NOW, List.of())));
  }

  @Test
  void exactTotalIncludesQuantityAndServerReplacesClientMetadata() {
    Instant before = Instant.now().minusMillis(1);
    var response = controller.pay(payment("30.00", "PLN", "111111"));
    Instant after = Instant.now();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(PaymentResult.success());
    List<Payment> stored = payments.findByReservationId(RESERVATION_ID);
    assertThat(stored).hasSize(1);
    Payment saved = stored.getFirst();
    assertThat(saved.id()).isNotEqualTo("integration-payment");
    assertThat(UUID.fromString(saved.id()).toString()).isEqualTo(saved.id());
    assertThat(saved.paymentTimestamp()).isBetween(before, after);
    assertThat(saved.amount()).isEqualTo("30.00");
    assertThat(payments.findById(saved.id())).contains(saved);
    assertStatus(ReservationStatus.PAID);
  }

  @Test
  void omittedIdAndTimestampArePopulated() {
    Payment original = payment("30.00", "PLN", "111111");
    Payment request = new Payment(null, RESERVATION_ID, original.amount(), original.currency(),
        original.paymentDetails(), null);
    assertThat(controller.pay(request).getStatusCode()).isEqualTo(HttpStatus.OK);
    Payment saved = payments.findByReservationId(RESERVATION_ID).getFirst();
    assertThat(saved.id()).isNotBlank();
    assertThat(saved.paymentTimestamp()).isNotNull();
  }

  @Test
  void underpaymentAndOverpaymentAreBadRequestsWithoutDatabaseChanges() {
    for (String amount : List.of("10.00", "40.00")) {
      var response = controller.pay(payment(amount, "PLN", "111111"));
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().statusMessage()).contains("does not match reservation balance");
    }
    assertNoPayment();
  }

  @Test
  void wrongCurrencyIsBadRequest() {
    var response = controller.pay(payment("30.00", "EUR", "111111"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().statusMessage()).contains("Currency mismatch");
    assertNoPayment();
  }

  @Test
  void invalidBlikCodeIsBadRequest() {
    assertThat(controller.pay(payment("30.00", "PLN", "123")).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
    assertNoPayment();
  }

  @Test
  void missingReservationIsNotFound() {
    Payment original = payment("30.00", "PLN", "111111");
    Payment missing = new Payment(null, "missing", original.amount(), original.currency(),
        original.paymentDetails(), null);
    assertThat(controller.pay(missing).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertNoPayment();
  }

  @Test
  void deniedBlikPaymentIsUnprocessable() {
    var response = controller.pay(payment("30.00", "PLN", "222222"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody().status()).isEqualTo(PaymentResult.PaymentStatus.DENIED);
    assertNoPayment();
  }

  @Test
  void failedBlikPaymentIsServerError() {
    var response = controller.pay(payment("30.00", "PLN", "444444"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().status()).isEqualTo(PaymentResult.PaymentStatus.FAILED);
    assertNoPayment();
  }

  @Test
  void finalizerCommitsPaymentAndReservationStatusTogether() {
    Payment payment = payment("30.00", "PLN", "111111");
    finalizer.finalize(payment);
    assertThat(payments.findById(payment.id())).contains(payment);
    assertStatus(ReservationStatus.PAID);
  }

  @Test
  void finalizerRollsBackInsertedPaymentWhenStatusTransitionFails() {
    jdbc.update("UPDATE reservation SET status = 'PAID' WHERE id = ?", RESERVATION_ID);
    Payment payment = payment("30.00", "PLN", "111111");
    assertThatThrownBy(() -> finalizer.finalize(payment))
        .isInstanceOf(RequestConflictException.class);
    assertThat(payments.findById(payment.id())).isEmpty();
    assertThat(payments.findByReservationId(RESERVATION_ID)).isEmpty();
    assertStatus(ReservationStatus.PAID);
  }

  private Payment payment(String amount, String currency, String code) {
    return new Payment("integration-payment", RESERVATION_ID, amount, currency,
        new PaymentDetails(PaymentMethod.BLIK, null, null,
            new BlikPaymentDetails(code, "buyer@example.org")), NOW);
  }

  private void assertNoPayment() {
    assertThat(payments.findByReservationId(RESERVATION_ID)).isEmpty();
    assertStatus(ReservationStatus.RESERVED);
  }

  private void assertStatus(ReservationStatus expected) {
    assertThat(reservations.findById(RESERVATION_ID)).hasValueSatisfying(
        reservation -> assertThat(reservation.status()).isEqualTo(expected));
  }
}
