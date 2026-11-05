package pl.edu.uj.student.wzorceprojektowe.reservation.model;

import java.time.Instant;
import java.util.List;

public record Reservation(
    String id,
    String clientId,
    List<OrderItem> items,
    Instant createTimestamp,
    Instant lastModifiedTimestamp) {
  public Reservation {
    items = List.copyOf(items);
  }
}
