package pl.edu.uj.student.wzorceprojektowe.payment.service;

import java.util.List;
import org.springframework.stereotype.Service;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;

@Service
public class PaymentAmountValidator {

  public void validateAmounts(Payment payment, Reservation reservation) {
    String reservationCurrency = findCurrency(reservation);
    Money reservationBalance =
            calculateReservationBalance(reservation, reservationCurrency);
    Money paymentAmount = new Money(payment.currency(), payment.amount());
    Money balanceAfterPayment;
    try {
      balanceAfterPayment = reservationBalance.subtract(paymentAmount);
    } catch (IllegalArgumentException e) {
      throw new RequestValidationException(
          "Currency mismatch. Payment: %s, Reservation: %s"
              .formatted(payment.currency(), reservationCurrency));
    }
    if (balanceAfterPayment.amount().signum() != 0) {
      throw new RequestValidationException(
          "Payment amount: %s does not match reservation balance: %s"
              .formatted(paymentAmount, reservationBalance));
    }
  }

  private static Money calculateReservationBalance(Reservation reservation, String reservationCurrency) {
    return reservation.items().stream()
            .map(it -> it.offerSnapshot().price())
            .reduce(Money.zero(reservationCurrency), Money::add);
  }

  private String findCurrency(Reservation reservation) {
    List<String> currencies =
        reservation.items().stream()
            .map(item -> item.offerSnapshot().price().currency())
            .distinct()
            .toList();
    if (currencies.isEmpty()) {
      throw new IllegalStateException("Cannot pay for non-priced reservation.");
    }
    if (currencies.size() > 1) {
      throw new IllegalStateException("Payment for multi-currency reservation is not supported");
    }
    return currencies.getFirst();
  }
}
