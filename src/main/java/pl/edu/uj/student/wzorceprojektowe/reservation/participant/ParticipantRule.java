package pl.edu.uj.student.wzorceprojektowe.reservation.participant;

import pl.edu.uj.student.wzorceprojektowe.shared.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Participant;
import pl.edu.uj.student.wzorceprojektowe.shared.model.ParticipantMode;

import java.util.List;

public interface ParticipantRule {
  ParticipantMode mode();

  void validate(Offer offer, int quantity, List<Participant> participants);
}
