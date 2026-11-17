package pl.edu.uj.student.wzorceprojektowe.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.uj.student.wzorceprojektowe.payment.model.PaymentResult;
import pl.edu.uj.student.wzorceprojektowe.payment.service.PaymentFacade;
import pl.edu.uj.student.wzorceprojektowe.payment.util.PaymentValidator;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentFacade paymentFacade;
  private final PaymentValidator paymentValidator;

  @PostMapping("/pay")
  public PaymentResult pay(@RequestBody Payment payment) {
    paymentValidator.validatePayment(payment);
    return paymentFacade.handlePayment(payment);
  }
}
