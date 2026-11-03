package pl.edu.uj.student.wzorceprojektowe.infrastructure;

import java.math.BigDecimal;
import org.springframework.boot.jackson.JacksonComponent;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;
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
      gen.writeStringProperty("currency", value.currency());
      gen.writeEndObject();
    }
  }

  public static class MoneyDeserializer extends ValueDeserializer<Money> {
    @Override
    public Money deserialize(JsonParser p, DeserializationContext ctxt) {
      JsonNode node = p.readValueAsTree();
      if (!node.isObject()) {
        throw new RequestValidationException(
            "price must be an object with 'amount' and 'currency' fields");
      }
      String amount = requiredString(node, "amount");
      String currency = requiredString(node, "currency");
      try {
        new BigDecimal(amount);
      } catch (NumberFormatException e) {
        throw new RequestValidationException(
            "price.amount must be a valid decimal number, got: \"" + amount + "\"");
      }
      return new Money(currency, amount);
    }

    private static String requiredString(JsonNode node, String field) {
      JsonNode value = node.get(field);
      if (value == null || !value.isString() || value.asString().isBlank()) {
        throw new RequestValidationException(
            "price." + field + " is required and must be a non-blank JSON string");
      }
      return value.asString();
    }
  }
}
