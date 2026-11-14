package pl.edu.uj.student.wzorceprojektowe.reservation.participant;

import org.springframework.stereotype.Component;
import pl.edu.uj.student.wzorceprojektowe.shared.model.ParticipantMode;

/**
 * Group tickets: the client's data is enough, so no participant data.
 */
@Component
public class GroupParticipantRule extends NoParticipantsRule {
  @Override
  public ParticipantMode mode() {
    return ParticipantMode.GROUP;
  }
}