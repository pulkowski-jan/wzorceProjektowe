package pl.edu.uj.student.wzorceprojektowe.shared.model;

import java.time.Instant;
import java.util.List;

public record OrderItem(
    String lineId,
    Offer offerSnapshot,
    int quantity,
    Instant timestamp,
    List<Participant> participants) {
  public OrderItem {
    participants = participants == null ? List.of() : List.copyOf(participants);
  }
}
