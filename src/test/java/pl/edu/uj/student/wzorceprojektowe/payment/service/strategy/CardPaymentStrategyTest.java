package pl.edu.uj.student.wzorceprojektowe.payment.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import pl.edu.uj.student.wzorceprojektowe.payment.model.PaymentResult;
import pl.edu.uj.student.wzorceprojektowe.payment.service.strategy.card.CardPaymentAdapter;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;

class CardPaymentStrategyTest {
  @Test
  void cancellationAlwaysSucceedsWithoutCallingGateway() {
    CardPaymentAdapter adapter = mock(CardPaymentAdapter.class);
    CardPaymentStrategy strategy = new CardPaymentStrategy(adapter);
    assertThat(strategy.cancelPayment(mock(Payment.class))).isEqualTo(PaymentResult.success());
    assertThat(strategy.cancelPayment(null)).isEqualTo(PaymentResult.success());
    verifyNoInteractions(adapter);
  }
}
