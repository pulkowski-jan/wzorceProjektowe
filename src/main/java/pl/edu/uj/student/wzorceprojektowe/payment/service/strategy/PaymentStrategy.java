package pl.edu.uj.student.wzorceprojektowe.payment.service.strategy;

import pl.edu.uj.student.wzorceprojektowe.payment.model.PaymentResult;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;

public interface PaymentStrategy {
    boolean isApplicableFor(Payment payment);
    PaymentResult handle(Payment payment, Reservation reservation);
    PaymentResult cancelPayment(Payment payment);
}
