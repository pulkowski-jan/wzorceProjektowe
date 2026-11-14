package pl.edu.uj.student.wzorceprojektowe.reservation.participant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Participant;
import pl.edu.uj.student.wzorceprojektowe.shared.model.ParticipantMode;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.PolicyExecutor;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy;

import java.util.List;
import java.util.function.Function;

import static pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy.*;

@Component
@RequiredArgsConstructor
public class NamedParticipantRule implements ParticipantRule {
  private static final List<ValidationPolicy<Participant>> PARTICIPANT_POLICIES =
      List.of(
          notNullPolicy(Function.identity(), "Participant must not be null"),
          notBlankPolicy(NamedParticipantRule::nameOf, "Participant name must not be blank"),
          maxLengthPolicy(
              NamedParticipantRule::nameOf, 255, "Participant name length may not exceed 255"));

  private final PolicyExecutor<Participant> policyExecutor;

  @Override
  public ParticipantMode mode() {
    return ParticipantMode.NAMED;
  }

  @Override
  public void validate(Offer offer, int quantity, List<Participant> participants) {
    if (participants.size() != quantity) {
      throw new RequestValidationException(
          "Offer %s requires %d participant(s), one per ticket, but %d given"
              .formatted(offer.id(), quantity, participants.size()));
    }
    for (Participant participant : participants) {
      policyExecutor.executePolicies(
          PARTICIPANT_POLICIES.stream(), participant, RequestValidationException::new);
    }
  }

  private static String nameOf(Participant participant) {
    return participant == null ? null : participant.name();
  }
}