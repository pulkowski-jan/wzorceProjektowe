package pl.edu.uj.student.wzorceprojektowe.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pl.edu.uj.student.wzorceprojektowe.payment.controller.PaymentController;
import pl.edu.uj.student.wzorceprojektowe.payment.model.PaymentResult;
import pl.edu.uj.student.wzorceprojektowe.payment.repository.PaymentRepository;
import pl.edu.uj.student.wzorceprojektowe.payment.service.PaymentFinalizer;
import pl.edu.uj.student.wzorceprojektowe.reservation.repository.ReservationRepository;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestConflictException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.*;
import org.springframework.jdbc.core.JdbcTemplate;

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
    Offer offer = new Offer("offer-1", "Test offer", null, new Money("PLN", "10.00"), "Test", 1, 1);
    reservations.storeItems(RESERVATION_ID,
        List.of(new OrderItem("line-1", offer, 1, NOW, List.of())));
  }

  @Test
  void exactBlikPaymentFlowsFromControllerToDatabase() {
    Payment payment = payment("10.00", "PLN", "111111");

    assertThat(controller.pay(payment)).isEqualTo(PaymentResult.success());
    assertThat(payments.findById(payment.id())).contains(payment);
    assertThat(payments.findByReservationId(RESERVATION_ID)).containsExactly(payment);
    assertStatus(ReservationStatus.PAID);
  }

  @Test
  void wrongCurrencyDoesNotPersistPaymentOrChangeReservation() {
    Payment payment = payment("10.00", "EUR", "111111");

    PaymentResult result = controller.pay(payment);

    assertThat(result.status()).isEqualTo(PaymentResult.PaymentStatus.FAILED);
    assertThat(result.statusMessage()).contains("Currency mismatch");
    assertThat(payments.findByReservationId(RESERVATION_ID)).isEmpty();
    assertStatus(ReservationStatus.RESERVED);
  }

  @Test
  void invalidBlikCodeIsRejectedBeforePaymentProcessing() {
    Payment payment = payment("10.00", "PLN", "123");

    assertThatThrownBy(() -> controller.pay(payment))
        .isInstanceOf(RequestValidationException.class);
    assertThat(payments.findByReservationId(RESERVATION_ID)).isEmpty();
    assertStatus(ReservationStatus.RESERVED);
  }

  @Test
  void finalizerCommitsPaymentAndReservationStatusTogether() {
    Payment payment = payment("10.00", "PLN", "111111");

    finalizer.finalize(payment);

    assertThat(payments.findById(payment.id())).contains(payment);
    assertStatus(ReservationStatus.PAID);
  }

  @Test
  void finalizerRollsBackInsertedPaymentWhenStatusTransitionFails() {
    jdbc.update("UPDATE reservation SET status = 'PAID' WHERE id = ?", RESERVATION_ID);
    Payment payment = payment("10.00", "PLN", "111111");

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

  private void assertStatus(ReservationStatus expected) {
    assertThat(reservations.findById(RESERVATION_ID)).hasValueSatisfying(
        reservation -> assertThat(reservation.status()).isEqualTo(expected));
  }
}
