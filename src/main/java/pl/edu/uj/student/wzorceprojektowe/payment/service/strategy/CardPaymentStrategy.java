package pl.edu.uj.student.wzorceprojektowe.payment.service.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.uj.student.wzorceprojektowe.payment.model.PaymentResult;
import pl.edu.uj.student.wzorceprojektowe.payment.service.strategy.card.CardPaymentAdapter;
import pl.edu.uj.student.wzorceprojektowe.shared.model.CardPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;
import pl.edu.uj.student.wzorceprojektowe.shared.model.PaymentMethod;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;

@Service
@RequiredArgsConstructor
class CardPaymentStrategy implements PaymentStrategy {

  private final CardPaymentAdapter cardPaymentAdapter;

  @Override
  public boolean isApplicableFor(Payment payment) {
    return payment.paymentDetails().paymentMethod() == PaymentMethod.CARD;
  }

  @Override
  public PaymentResult handle(Payment payment, Reservation reservation) {
    CardPaymentDetails cardPaymentDetails = payment.paymentDetails().cardPayment();
    try {
      cardPaymentAdapter.authorise(cardPaymentDetails);
    } catch (Exception e) {
      return PaymentResult.denial(e.getMessage());
    }
    try {
      cardPaymentAdapter.confirm(payment, reservation);
    } catch (Exception e) {
      return PaymentResult.failure(e.getMessage());
    }
    return PaymentResult.success();
  }

  @Override
  public PaymentResult cancelPayment(Payment payment) {
    throw new UnsupportedOperationException("Payment is not yet supported");
  }
}
