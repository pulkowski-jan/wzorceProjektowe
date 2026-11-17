package pl.edu.uj.student.wzorceprojektowe.payment.service.strategy.card.externalgateway;

import java.time.LocalDate;
import java.util.List;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;

public class ExternalCardGateway {
  private static final Card SUCCESS_CARD =
      new Card("1111111111111111", LocalDate.of(2027, 10, 1), "111");
  private static final Card FAILING_CARD =
      new Card("1234123412341234", LocalDate.of(2027, 10, 1), "1234");
  private static final List<Card> MOCK_AUTHORISED_CARDS = List.of(SUCCESS_CARD, FAILING_CARD);

  public void authorise(Card card) {
    if (!LocalDate.now().isBefore(card.expiryDate())) {
      throw new IllegalStateException("Provided card has already expired.");
    }
    if (!MOCK_AUTHORISED_CARDS.contains(card)) {
      throw new IllegalStateException("Card payment authorisation failure.");
    }
  }

  public void confirm(Card card, Money amount) {
      if (card.equals(FAILING_CARD)) {
          throw new IllegalStateException("Card operator unreachable");
      }
      if (!card.equals(SUCCESS_CARD)) {
          throw new IllegalStateException("Card payment has not been previously authorised");
      }
  }
}
