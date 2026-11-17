package pl.edu.uj.student.wzorceprojektowe.payment.service.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.uj.student.wzorceprojektowe.payment.model.PaymentResult;
import pl.edu.uj.student.wzorceprojektowe.payment.service.strategy.blik.BlikPaymentGateway;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;
import pl.edu.uj.student.wzorceprojektowe.shared.model.PaymentMethod;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;

@Service
@RequiredArgsConstructor
class BlikPaymentStrategy implements PaymentStrategy {
  private final BlikPaymentGateway blikPaymentGateway;

  @Override
  public boolean isApplicableFor(Payment payment) {
    return payment.paymentDetails().paymentMethod() == PaymentMethod.BLIK;
  }

  @Override
  public PaymentResult handle(Payment payment, Reservation reservation) {
    return blikPaymentGateway.pay(payment);
  }

  @Override
  public PaymentResult cancelPayment(Payment payment) {
    return PaymentResult.success();
  }
}
