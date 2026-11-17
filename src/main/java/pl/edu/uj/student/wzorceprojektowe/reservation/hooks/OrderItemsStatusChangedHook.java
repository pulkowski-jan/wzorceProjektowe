package pl.edu.uj.student.wzorceprojektowe.reservation.hooks;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import pl.edu.uj.student.wzorceprojektowe.shared.model.OrderItem;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;
import pl.edu.uj.student.wzorceprojektowe.shared.model.ReservationStatus;

@Component
public class OrderItemsStatusChangedHook implements ReservationUpdateObserver {

  @Override
  public Reservation handleReservationUpdate(
      Reservation oldReservation, Reservation updatedReservation) {
    Set<ReservationStatus> oldItemsStatus = getOrderItemsStatus(oldReservation);
    Set<ReservationStatus> updatedItemsStatus = getOrderItemsStatus(updatedReservation);
    if (oldItemsStatus.equals(updatedItemsStatus)) {
      return updatedReservation;
    }
    ReservationStatus updatedStatus =
        resolveStatus(updatedItemsStatus.stream().distinct().toList());
    if (updatedStatus.equals(updatedReservation.status())) {
      return updatedReservation;
    }
    return new Reservation(
        updatedReservation.id(),
        updatedReservation.clientId(),
        updatedReservation.items(),
        updatedStatus,
        updatedReservation.createTimestamp(),
        updatedReservation.lastModifiedTimestamp());
  }

  private static Set<ReservationStatus> getOrderItemsStatus(Reservation reservation) {
    return reservation.items().stream()
        .map(OrderItem::orderItemReservationStatus)
        .collect(Collectors.toSet());
  }

  ReservationStatus resolveStatus(List<ReservationStatus> itemsStatus) {
    if (itemsStatus.size() == 1) {
      return itemsStatus.getFirst();
    }
    if (itemsStatus.size() == 2
        && itemsStatus.contains(ReservationStatus.CANCELED)
        && itemsStatus.contains(ReservationStatus.PAID)) {
      return ReservationStatus.PAID;
    }
    return ReservationStatus.RESERVED;
  }
}
