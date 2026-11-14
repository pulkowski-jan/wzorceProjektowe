package pl.edu.uj.student.wzorceprojektowe.reservation.participant;

import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Participant;

import java.util.List;

abstract class NoParticipantsRule implements ParticipantRule {
  @Override
  public void validate(Offer offer, int quantity, List<Participant> participants) {
    if (!participants.isEmpty()) {
      throw new RequestValidationException(
          "Offer %s (%s) does not accept participants".formatted(offer.id(), mode())
      );
    }
  }
}
