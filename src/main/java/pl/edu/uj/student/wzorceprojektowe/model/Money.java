package pl.edu.uj.student.wzorceprojektowe.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class Money {
    private final String currency;
    private final BigDecimal amount;

    private Money(String currency, BigDecimal amount) {
        this.amount = amount;
        this.currency = currency;
    }

    @JsonCreator
    public Money(@JsonProperty("currency") String currency, @JsonProperty("amount") double amount) {
        this(currency, new BigDecimal(amount));
    }

    public Money(String currency, String amount) {
        this(currency, new BigDecimal(amount));
    }

    public String getCurrency() {
        return currency;
    }

    @JsonIgnore
    public double getAmount() {
        return amount.doubleValue();
    }

    @JsonProperty("amount")
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
            throw new IllegalArgumentException("Cannot add money of different currencies: %s != %s"
                    .formatted(this.currency, money.getCurrency()));
        }
    }
}
