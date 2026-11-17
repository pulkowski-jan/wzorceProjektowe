package pl.edu.uj.student.wzorceprojektowe.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.edu.uj.student.wzorceprojektowe.payment.model.PaymentResult;
import pl.edu.uj.student.wzorceprojektowe.payment.service.strategy.PaymentStrategy;
import pl.edu.uj.student.wzorceprojektowe.reservation.service.ReservationService;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;

class PaymentFacadeTest {
  private final ReservationService reservations = mock(ReservationService.class);
  private final PaymentAmountValidator amounts = mock(PaymentAmountValidator.class);
  private final PaymentFinalizer finalizer = mock(PaymentFinalizer.class);
  private final PaymentStrategy strategy = mock(PaymentStrategy.class);
  private final Payment payment = mock(Payment.class);
  private final Reservation reservation = mock(Reservation.class);
  private PaymentFacade facade;

  @BeforeEach
  void setUp() {
    when(payment.reservationId()).thenReturn("reservation-1");
    when(reservations.findById("reservation-1")).thenReturn(reservation);
    when(strategy.isApplicableFor(payment)).thenReturn(true);
    facade = new PaymentFacade(reservations, amounts, List.of(strategy), finalizer);
  }

  @Test
  void successfulPaymentIsValidatedChargedAndFinalizedInOrder() {
    when(strategy.handle(payment, reservation)).thenReturn(PaymentResult.success());

    assertThat(facade.handlePayment(payment)).isEqualTo(PaymentResult.success());

    var order = inOrder(reservations, amounts, strategy, finalizer);
    order.verify(reservations).findById("reservation-1");
    order.verify(amounts).validateAmounts(payment, reservation);
    order.verify(strategy).isApplicableFor(payment);
    order.verify(strategy).handle(payment, reservation);
    order.verify(finalizer).finalize(payment);
    verify(strategy, never()).cancelPayment(any());
  }

  @Test
  void invalidAmountStopsBeforeChargingOrFinalizing() {
    doThrow(new RequestValidationException("Amount mismatch"))
        .when(amounts).validateAmounts(payment, reservation);

    assertThat(facade.handlePayment(payment))
        .isEqualTo(PaymentResult.failure("Amount mismatch"));
    verifyNoInteractions(strategy, finalizer);
  }

  @Test
  void deniedPaymentIsReturnedWithoutFinalizationOrCancellation() {
    PaymentResult denied = PaymentResult.denial("Not authorised");
    when(strategy.handle(payment, reservation)).thenReturn(denied);

    assertThat(facade.handlePayment(payment)).isSameAs(denied);
    verifyNoInteractions(finalizer);
    verify(strategy, never()).cancelPayment(any());
  }

  @Test
  void gatewayExceptionBecomesFailureWithoutFinalization() {
    when(strategy.handle(payment, reservation)).thenThrow(new IllegalStateException("Offline"));

    assertThat(facade.handlePayment(payment)).isEqualTo(PaymentResult.failure("Offline"));
    verifyNoInteractions(finalizer);
    verify(strategy, never()).cancelPayment(any());
  }

  @Test
  void finalizationFailureCancelsSuccessfulCharge() {
    when(strategy.handle(payment, reservation)).thenReturn(PaymentResult.success());
    doThrow(new IllegalStateException("Database unavailable")).when(finalizer).finalize(payment);
    when(strategy.cancelPayment(payment)).thenReturn(PaymentResult.success());

    assertThat(facade.handlePayment(payment))
        .isEqualTo(PaymentResult.failure("Payment cancelled, due to: Database unavailable"));
    var order = inOrder(strategy, finalizer);
    order.verify(strategy).handle(payment, reservation);
    order.verify(finalizer).finalize(payment);
    order.verify(strategy).cancelPayment(payment);
  }

  @Test
  void unsuccessfulCancellationPreservesBothFailureReasons() {
    when(strategy.handle(payment, reservation)).thenReturn(PaymentResult.success());
    doThrow(new IllegalStateException("Database unavailable")).when(finalizer).finalize(payment);
    when(strategy.cancelPayment(payment)).thenReturn(PaymentResult.failure("Refund rejected"));

    assertThat(facade.handlePayment(payment)).isEqualTo(PaymentResult.failure(
        "Payment failed, due to: Database unavailable. Subsequent cancel failed due to: Refund rejected."));
  }

  @Test
  void cancellationExceptionBecomesFailureWithBothReasons() {
    when(strategy.handle(payment, reservation)).thenReturn(PaymentResult.success());
    doThrow(new IllegalStateException("Database unavailable")).when(finalizer).finalize(payment);
    when(strategy.cancelPayment(payment)).thenThrow(new IllegalStateException("Refund offline"));

    assertThat(facade.handlePayment(payment)).isEqualTo(PaymentResult.failure(
        "Payment failed, due to: Database unavailable. Subsequent cancel failed due to: Refund offline."));
  }

  @Test
  void unsupportedPaymentIsRejectedBeforeCharging() {
    when(strategy.isApplicableFor(payment)).thenReturn(false);

    assertThatThrownBy(() -> facade.handlePayment(payment))
        .isInstanceOf(RequestValidationException.class)
        .hasMessage("Provided payment is unsupported");
    verify(strategy, never()).handle(any(), any());
    verifyNoInteractions(finalizer);
  }

  @Test
  void multipleMatchingStrategiesAreRejectedBeforeCharging() {
    PaymentStrategy other = mock(PaymentStrategy.class);
    when(other.isApplicableFor(payment)).thenReturn(true);
    facade = new PaymentFacade(reservations, amounts, List.of(strategy, other), finalizer);

    assertThatThrownBy(() -> facade.handlePayment(payment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("multiple applicable payment strategies");
    verify(strategy, never()).handle(any(), any());
    verify(other, never()).handle(any(), any());
    verifyNoInteractions(finalizer);
  }

  @Test
  void selectsMatchingStrategyEvenWhenItIsNotFirst() {
    PaymentStrategy unsupported = mock(PaymentStrategy.class);
    when(unsupported.isApplicableFor(payment)).thenReturn(false);
    when(strategy.handle(payment, reservation)).thenReturn(PaymentResult.success());
    facade = new PaymentFacade(reservations, amounts, List.of(unsupported, strategy), finalizer);

    assertThat(facade.handlePayment(payment)).isEqualTo(PaymentResult.success());
    verify(strategy).handle(payment, reservation);
    verify(unsupported, never()).handle(any(), any());
    verify(finalizer).finalize(payment);
  }
}
