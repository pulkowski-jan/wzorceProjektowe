package pl.edu.uj.student.wzorceprojektowe.reservation.participant;

import org.springframework.stereotype.Component;
import pl.edu.uj.student.wzorceprojektowe.shared.model.ParticipantMode;

@Component
public class NoneParticipantRule extends NoParticipantsRule {
  @Override
  public ParticipantMode mode() {
    return ParticipantMode.NONE;
  }
}
