package pl.edu.uj.student.wzorceprojektowe.payment.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.uj.student.wzorceprojektowe.payment.model.PaymentResult;
import pl.edu.uj.student.wzorceprojektowe.payment.service.strategy.PaymentStrategy;
import pl.edu.uj.student.wzorceprojektowe.reservation.exception.ReservationNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.reservation.service.ReservationService;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;

@Service
@RequiredArgsConstructor
public class PaymentFacade {
  private final ReservationService reservationService;
  private final PaymentAmountValidator paymentAmountValidator;
  private final List<PaymentStrategy> paymentStrategies;
  private final PaymentFinalizer paymentFinalizer;

  public PaymentResult handlePayment(Payment payment) {
    Reservation reservation = findReservation(payment);
    try {
      paymentAmountValidator.validateAmounts(payment, reservation);
    } catch (Exception e) {
      return PaymentResult.failure(e.getMessage());
    }
    PaymentStrategy paymentStrategy = resolvePaymentStrategy(payment);
    PaymentResult paymentResult;
    try {
      paymentResult = paymentStrategy.handle(payment, reservation);
    } catch (Exception e) {
      paymentResult = PaymentResult.failure(e.getMessage());
    }
    if (!paymentResult.successfull()) {
      return paymentResult;
    }
    try {
      paymentFinalizer.finalize(payment);
      return PaymentResult.success();
    } catch (Exception e) {
      return cancelPayment(payment, e, paymentStrategy);
    }
  }

  private Reservation findReservation(Payment payment) {
    Reservation reservation;
    try {
      reservation = reservationService.findById(payment.reservationId());
    } catch (ReservationNotFoundException e) {
      throw new RequestValidationException(
          "Cannot pay for non-existent reservation: " + payment.reservationId());
    }
    return reservation;
  }

  private PaymentResult cancelPayment(
      Payment payment, Exception paymentFinalizationError, PaymentStrategy paymentStrategy) {
    PaymentResult cancelResult;
    try {
      cancelResult = paymentStrategy.cancelPayment(payment);
    } catch (Exception e) {
      cancelResult = PaymentResult.failure(e.getMessage());
    }
    if (cancelResult.successfull()) {
      return PaymentResult.failure(
          "Payment cancelled, due to: " + paymentFinalizationError.getMessage());
    } else {
      return PaymentResult.failure(
          "Payment failed, due to: %s. Subsequent cancel failed due to: %s."
              .formatted(paymentFinalizationError.getMessage(), cancelResult.statusMessage()));
    }
  }

  private PaymentStrategy resolvePaymentStrategy(Payment payment) {
    List<PaymentStrategy> applicablePaymentStrategies =
        paymentStrategies.stream().filter(it -> it.isApplicableFor(payment)).toList();
    if (applicablePaymentStrategies.isEmpty()) {
      throw new RequestValidationException("Provided payment is unsupported");
    }
    if (applicablePaymentStrategies.size() != 1) {
      throw new IllegalStateException(
          "Server misconfiguration: multiple applicable payment strategies for payment "
              + payment.id());
    }
    return applicablePaymentStrategies.getFirst();
  }
}
