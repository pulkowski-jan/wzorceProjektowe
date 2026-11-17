package pl.edu.uj.student.wzorceprojektowe.payment.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentResultTest {
  @Test
  void successHasEmptyRatherThanNullMessage() {
    PaymentResult result = PaymentResult.success();
    assertThat(result.status()).isEqualTo(PaymentResult.PaymentStatus.SUCCESSFUL);
    assertThat(result.statusMessage()).isNotNull().isEmpty();
    assertThat(result.successfull()).isTrue();
  }
}
