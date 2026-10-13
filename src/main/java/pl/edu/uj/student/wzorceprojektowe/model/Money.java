package pl.edu.uj.student.wzorceprojektowe.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.math.BigDecimal;
import java.util.Objects;

public class Money {
    private final String currency;
    private final BigDecimal amount;

    @JsonCreator
    public Money(String currency, double amount) {
        this.currency = currency;
        this.amount = new BigDecimal(amount);
    }

    private Money(String currency, BigDecimal amount) {
        this.amount = amount;
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }

    public double getAmount() {
        return amount.doubleValue();
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
            throw new IllegalArgumentException("Cannot add money of different currencies: %s != %s"
                    .formatted(this.currency, money.getCurrency()));
        }
    }
}
