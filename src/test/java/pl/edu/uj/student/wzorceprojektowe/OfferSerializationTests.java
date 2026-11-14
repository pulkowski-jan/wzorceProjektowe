package pl.edu.uj.student.wzorceprojektowe;

import org.junit.jupiter.api.Test;
import pl.edu.uj.student.wzorceprojektowe.infrastructure.MoneyJacksonComponent;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.shared.model.ParticipantMode;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Vendor;
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
                new Money("PLN", "12.30"), "Ticket", 1, 1);
        var json = mapper.readTree(mapper.writeValueAsString(offer));
        assertTrue(json.get("price").get("amount").isString());
        assertEquals("12.30", json.get("price").get("amount").asString());
        assertEquals(2, json.get("price").size());
    }

    @Test
    void participantModeDefaultsToNoneWhenAbsentAndRoundTripsWhenPresent() {
        var legacy = mapper.readValue(
            "{\"name\":\"Concert\",\"vendor\":{\"name\":\"V\"},"
                + "\"price\":{\"amount\":\"1.00\",\"currency\":\"PLN\"},\"quantity\":1}",
            Offer.class);
        assertEquals(ParticipantMode.NONE, legacy.participantMode());
        assertEquals("NONE", mapper.readTree(mapper.writeValueAsString(legacy))
            .get("participantMode").asString());

        var named = new Offer("id", "Concert", new Vendor("V"), new Money("PLN", "1.00"), "", 1, 1,
            ParticipantMode.NAMED);
        var copy = mapper.readValue(mapper.writeValueAsString(named), Offer.class);
        assertEquals(ParticipantMode.NAMED, copy.participantMode());
        assertEquals(named, copy);
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
}
