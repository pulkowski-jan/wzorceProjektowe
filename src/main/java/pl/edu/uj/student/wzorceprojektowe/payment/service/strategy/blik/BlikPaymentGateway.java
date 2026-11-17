package pl.edu.uj.student.wzorceprojektowe.payment.service.strategy.blik;

import org.springframework.stereotype.Component;
import pl.edu.uj.student.wzorceprojektowe.payment.model.PaymentResult;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;

@Component
public class BlikPaymentGateway {
  private static final String ACCEPTED_CODE = "111111";
  private static final String DENIED_CODE = "222222";
  private static final String EXPIRED_CODE = "333333";
  private static final String FAILING_CODE = "444444";

  public PaymentResult pay(Payment payment) {
    String blikCode = payment.paymentDetails().blikPayment().blikCode();
    return switch (blikCode) {
      case ACCEPTED_CODE -> PaymentResult.success();
      case DENIED_CODE -> PaymentResult.denial("Payment not authorised in time.");
      case EXPIRED_CODE -> PaymentResult.denial("Code has already expired.");
      case FAILING_CODE -> PaymentResult.failure("BLIK operator unreachable.");
      default -> PaymentResult.denial("Unknown code.");
    };
  }
}
