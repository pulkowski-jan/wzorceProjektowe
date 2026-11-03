package pl.edu.uj.student.wzorceprojektowe.shared.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(String currency, BigDecimal amount) {
  private static final int SCALE = 2;
  private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

  public Money(String currency, BigDecimal amount) {
    if (currency == null || currency.isBlank()) {
      throw new IllegalArgumentException("Currency must not be blank");
    }

    this.currency = currency;
    this.amount =
        Objects.requireNonNull(amount, "Amount must not be null").setScale(SCALE, ROUNDING);
  }

  public Money(String currency, String amount) {
    this(currency, new BigDecimal(Objects.requireNonNull(amount, "Amount must not be null")));
  }

  public String getAmountAsString() {
    return amount.toPlainString();
  }

  public Money add(Money other) {
    requireSameCurrency(other);
    return new Money(currency, amount.add(other.amount));
  }

  public Money subtract(Money other) {
    requireSameCurrency(other);
    return new Money(currency, amount.subtract(other.amount));
  }

  public Money negate() {
    return new Money(currency, amount.negate());
  }

  public Money multiply(BigDecimal factor) {
    Objects.requireNonNull(factor, "Factor must not be null");
    return new Money(currency, amount.multiply(factor));
  }

  private void requireSameCurrency(Money other) {
    Objects.requireNonNull(other, "Money must not be null");

    if (!currency.equals(other.currency)) {
      throw new IllegalArgumentException(
          "Currency mismatch: %s != %s".formatted(currency, other.currency));
    }
  }
}
