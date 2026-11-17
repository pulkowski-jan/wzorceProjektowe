package pl.edu.uj.student.wzorceprojektowe.payment.service.strategy;

import java.util.List;
import org.springframework.stereotype.Service;
import pl.edu.uj.student.wzorceprojektowe.payment.model.PaymentResult;
import pl.edu.uj.student.wzorceprojektowe.shared.model.*;

@Service
class CashPaymentStrategy implements PaymentStrategy {

  private static final String UNAUTHORISED_AGENT_MESSAGE =
      "Only vendor agents are authorised to perform cash payments.";

  @Override
  public boolean isApplicableFor(Payment payment) {
    return payment.paymentDetails().paymentMethod() == PaymentMethod.CASH;
  }

  @Override
  public PaymentResult handle(Payment payment, Reservation reservation) {
    CashPaymentDetails cashPaymentDetails = payment.paymentDetails().cashPayment();
    Vendor vendor = findVendor(reservation);
    if (!cashPaymentDetails.agentCode().startsWith(vendor.name())) {
      return PaymentResult.denial(UNAUTHORISED_AGENT_MESSAGE);
    }
    return PaymentResult.success();
  }

  private static Vendor findVendor(Reservation reservation) {
    List<Vendor> vendors =
        reservation.items().stream().map(it -> it.offerSnapshot().vendor()).distinct().toList();
    if (vendors.size() != 1) {
      throw new IllegalStateException("Cash payment for multi-vendor reservation is unsupported.");
    }
    return vendors.getFirst();
  }

  @Override
  public PaymentResult cancelPayment(Payment payment) {
    return PaymentResult.success();
  }
}
