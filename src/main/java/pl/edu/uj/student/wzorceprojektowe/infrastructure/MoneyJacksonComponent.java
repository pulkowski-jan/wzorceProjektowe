package pl.edu.uj.student.wzorceprojektowe.infrastructure;

import org.springframework.boot.jackson.JacksonComponent;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

@JacksonComponent
public class MoneyJacksonComponent {

  public static class MoneySerializer extends ValueSerializer<Money> {
    @Override
    public void serialize(Money value, JsonGenerator gen, SerializationContext ctxt) {
      gen.writeStartObject();
      gen.writeStringProperty("amount", value.getAmountAsString());
      gen.writeStringProperty("currency", value.getCurrency());
      gen.writeEndObject();
    }
  }

  public static class MoneyDeserializer extends ValueDeserializer<Money> {
    @Override
    public Money deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
      JsonNode node = p.readValueAsTree();
      String currency = node.get("currency").asString();
      String amount = node.get("amount").asString();
      return new Money(currency, amount);
    }
  }
}
