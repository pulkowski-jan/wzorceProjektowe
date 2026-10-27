package pl.edu.uj.student.wzorceprojektowe;

import org.junit.jupiter.api.Test;
import pl.edu.uj.student.wzorceprojektowe.infrastructure.MoneyJacksonComponent;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Vendor;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.*;

class OfferSerializationTests {
    private final SimpleModule moneyModule = new SimpleModule()
            .addSerializer(Money.class, new MoneyJacksonComponent.MoneySerializer())
            .addDeserializer(Money.class, new MoneyJacksonComponent.MoneyDeserializer());
    private final JsonMapper mapper = JsonMapper.builder().addModule(moneyModule).build();

    @Test
    void priceAmountIsAStringAndPreservesDecimalScale() {
        var offer = new Offer("test-id", "Concert", new Vendor("Vendor"),
                new Money("PLN", "12.30"), "Ticket", 1);
        var json = mapper.readTree(mapper.writeValueAsString(offer));
        assertTrue(json.get("price").get("amount").isString());
        assertEquals("12.30", json.get("price").get("amount").asString());
        assertEquals(2, json.get("price").size());
    }

    @Test
    void amountsAlwaysHaveTwoDecimalPlaces() {
        for (var example : new String[][] {
                {"120.0", "120.00"}, {"120", "120.00"},
                {"0", "0.00"}, {"12.345", "12.35"}
        }) {
            var json = mapper.readTree(mapper.writeValueAsString(new Money("PLN", example[0])));
            assertEquals(example[1], json.get("amount").asString());
        }
    }

    @Test
    void numericAmountsCanStillBeDeserialized() {
        var money = mapper.readValue("{\"currency\":\"PLN\",\"amount\":12.5}", Money.class);
        assertEquals(12.5, money.getAmount());
    }
}
