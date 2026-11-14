package pl.edu.uj.student.wzorceprojektowe.reservation.participant;

import org.springframework.stereotype.Component;
import pl.edu.uj.student.wzorceprojektowe.shared.model.ParticipantMode;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ParticipantRuleRegistry {
  private final Map<ParticipantMode, ParticipantRule> rules = new EnumMap<>(ParticipantMode.class);

  public ParticipantRuleRegistry(List<ParticipantRule> available) {
    for (ParticipantRule rule : available) {
      if (rules.putIfAbsent(rule.mode(), rule) != null) {
        throw new IllegalStateException("Duplicate participant rule for mode " + rule.mode());
      }
    }
    for (ParticipantMode mode : ParticipantMode.values()) {
      if (!rules.containsKey(mode)) {
        throw new IllegalStateException("No participant rule for mode " + mode);
      }
    }
  }

  public ParticipantRule forMode(ParticipantMode mode) {
    return rules.get(mode);
  }
}
