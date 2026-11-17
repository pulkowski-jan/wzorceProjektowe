package pl.edu.uj.student.wzorceprojektowe.shared.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record OrderItem(
    String lineId,
    Offer offerSnapshot,
    int quantity,
    Instant timestamp,
    List<Participant> participants,
    ReservationStatus orderItemReservationStatus) {
  public OrderItem {
    participants = participants == null ? List.of() : List.copyOf(participants);
    orderItemReservationStatus =
        Objects.requireNonNullElse(orderItemReservationStatus, ReservationStatus.RESERVED);
  }
}
