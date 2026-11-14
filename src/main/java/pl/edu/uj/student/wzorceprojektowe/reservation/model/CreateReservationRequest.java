package pl.edu.uj.student.wzorceprojektowe.reservation.model;

import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Participant;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;
import java.util.List;

public record CreateReservationRequest(String clientId, List<Item> items) {
  public record Item(
      String offerId,
      @JsonDeserialize(using = QuantityDeserializer.class) Integer quantity, List<Participant> participants) {
    public Item {
      participants = participants == null ? List.of() : Collections.unmodifiableList(participants);
    }
  }

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
