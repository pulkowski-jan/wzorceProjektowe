package pl.edu.uj.student.wzorceprojektowe.reservation.model;

import java.util.List;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

public record CreateReservationRequest(String clientId, List<Item> items) {
  public record Item(
      String offerId,
      @JsonDeserialize(using = QuantityDeserializer.class) Integer quantity) {}

  public static class QuantityDeserializer extends ValueDeserializer<Integer> {
    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) {
      if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
        throw new RequestValidationException("quantity must be an integer");
      }
      return parser.getIntValue();
    }
  }
}
