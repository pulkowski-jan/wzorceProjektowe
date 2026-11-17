package pl.edu.uj.student.wzorceprojektowe.reservation.hooks;

import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.edu.uj.student.wzorceprojektowe.offer.service.OfferChangeService;
import pl.edu.uj.student.wzorceprojektowe.shared.model.OrderItem;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;
import pl.edu.uj.student.wzorceprojektowe.shared.model.ReservationStatus;

@Component
@RequiredArgsConstructor
public class OrderItemsCancelledHook implements ReservationUpdateObserver {
  private final OfferChangeService offerChangeService;

  @Override
  public Reservation handleReservationUpdate(
      Reservation oldReservation, Reservation updatedReservation) {
    Set<String> oldCancelledItems = getCancelledOrderItemsIds(oldReservation);
    Set<String> updatedCancelledItems =
        new HashSet<>(getCancelledOrderItemsIds(updatedReservation));
    updatedCancelledItems.removeAll(oldCancelledItems);
    if (!updatedCancelledItems.isEmpty()) {
      releaseStock(updatedReservation, updatedCancelledItems);
    }
    return updatedReservation;
  }

  private static Set<String> getCancelledOrderItemsIds(Reservation reservation) {
    return reservation.items().stream()
        .filter(item -> item.orderItemReservationStatus() == ReservationStatus.CANCELED)
        .map(OrderItem::lineId)
        .collect(Collectors.toSet());
  }

  private void releaseStock(Reservation reservation, Set<String> cancelledOrderItemIds) {
    Map<String, Integer> quantities = new LinkedHashMap<>();
    for (OrderItem item : reservation.items()) {
      if (cancelledOrderItemIds.contains(item.lineId())) {
        quantities.merge(item.offerSnapshot().id(), item.quantity(), Integer::sum);
      }
    }
    quantities.forEach(offerChangeService::releaseQuantity);
  }
}
