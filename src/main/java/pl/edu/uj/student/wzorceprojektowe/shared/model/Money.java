package pl.edu.uj.student.wzorceprojektowe.shared.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import lombok.Getter;

public class Money {
  @Getter private final String currency;
  private final BigDecimal amount;

  private Money(String currency, BigDecimal amount) {
    this.amount = amount;
    this.currency = currency;
  }

  public Money(String currency, double amount) {
    this(currency, new BigDecimal(amount));
  }

  public Money(String currency, String amount) {
    this(currency, new BigDecimal(amount));
  }

  public double getAmount() {
    return amount.doubleValue();
  }

  public String getAmountAsString() {
    return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  public Money add(Money money) {
    validateForArithmetics(money);
    return new Money(currency, amount.add(money.amount));
  }

  public Money negate() {
    return new Money(currency, amount.negate());
  }

  public Money subtract(Money money) {
    return add(money.negate());
  }

  public Money multiply(double times) {
    return new Money(currency, amount.multiply(new BigDecimal(times)));
  }

  private void validateForArithmetics(Money money) {
    Objects.requireNonNull(money, "Money must not be null");
    if (!this.currency.equals(money.getCurrency())) {
      throw new IllegalArgumentException(
          "Cannot add money of different currencies: %s != %s"
              .formatted(this.currency, money.getCurrency()));
    }
  }
}
