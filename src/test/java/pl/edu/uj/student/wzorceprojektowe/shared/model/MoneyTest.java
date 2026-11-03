package pl.edu.uj.student.wzorceprojektowe.shared.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void normalizesAtConstruction() {
        Money money = new Money("PLN", "1.005");

        assertThat(money.amount()).isEqualTo(new BigDecimal("1.01"));
        assertThat(money.getAmountAsString()).isEqualTo("1.01");
    }

    @Test
    void preservesValueAcrossStorageRepresentationRoundTrip() {
        Money original = new Money("PLN", "1.005");
        Money restored =
                new Money(original.currency(), original.getAmountAsString());

        assertThat(restored).isEqualTo(original);
        assertThat(restored.hashCode()).isEqualTo(original.hashCode());
    }

    @Test
    void equalityDoesNotDependOnInputScale() {
        Money first = new Money("PLN", "1");
        Money second = new Money("PLN", "1.00");

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void roundsMultiplicationUsingTheSamePolicy() {
        Money result =
                new Money("PLN", "0.05").multiply(new BigDecimal("0.5"));

        assertThat(result).isEqualTo(new Money("PLN", "0.03"));
    }

    @Test
    void rejectsMixedCurrencyArithmetic() {
        assertThatThrownBy(
                () -> new Money("PLN", "1.00").add(new Money("EUR", "1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankCurrency() {
        assertThatThrownBy(() -> new Money(" ", "1.00"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void supportsNegativeAmounts() {
        assertThat(new Money("PLN", "1.00").negate())
                .isEqualTo(new Money("PLN", "-1.00"));
    }
}