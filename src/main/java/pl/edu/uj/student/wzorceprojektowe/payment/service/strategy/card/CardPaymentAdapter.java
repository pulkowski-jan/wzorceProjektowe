package pl.edu.uj.student.wzorceprojektowe.payment.service.strategy.card;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import pl.edu.uj.student.wzorceprojektowe.payment.service.strategy.card.externalgateway.Card;
import pl.edu.uj.student.wzorceprojektowe.payment.service.strategy.card.externalgateway.ExternalCardGateway;
import pl.edu.uj.student.wzorceprojektowe.shared.model.CardPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;

@Service
public class CardPaymentAdapter {
  private final ExternalCardGateway externalCardGateway = new ExternalCardGateway();

  public void authorise(CardPaymentDetails cardPaymentDetails) {
    externalCardGateway.authorise(buildCard(cardPaymentDetails));
  }

  public void confirm(Payment payment, Reservation reservation) {
    externalCardGateway.confirm(
        buildCard(payment.paymentDetails().cardPayment()),
        new Money(payment.currency(), payment.amount()));
  }

  private Card buildCard(CardPaymentDetails cardPaymentDetails) {
    return new Card(
        cardPaymentDetails.cardNumber(),
        resolveCardExpiryDate(cardPaymentDetails),
        cardPaymentDetails.securityCode());
  }

  private LocalDate resolveCardExpiryDate(CardPaymentDetails cardPaymentDetails) {
    return LocalDate.of(
            cardPaymentDetails.expirationYear(), cardPaymentDetails.expirationMonth(), 1)
        .plusMonths(1);
  }
}
