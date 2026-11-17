package pl.edu.uj.student.wzorceprojektowe.reservation.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.uj.student.wzorceprojektowe.offer.service.OfferChangeService;
import pl.edu.uj.student.wzorceprojektowe.reservation.client.exception.ClientNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.reservation.client.service.ClientService;
import pl.edu.uj.student.wzorceprojektowe.reservation.exception.ReservationNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.reservation.hooks.ReservationUpdateObserver;
import pl.edu.uj.student.wzorceprojektowe.reservation.model.CreateReservationRequest;
import pl.edu.uj.student.wzorceprojektowe.reservation.participant.ParticipantRuleRegistry;
import pl.edu.uj.student.wzorceprojektowe.reservation.repository.ReservationRepository;
import pl.edu.uj.student.wzorceprojektowe.reservation.util.CreateReservationRequestValidator;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestConflictException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.shared.model.OrderItem;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;
import pl.edu.uj.student.wzorceprojektowe.shared.model.ReservationStatus;

@Service
@RequiredArgsConstructor
public class ReservationService {
  private final ReservationRepository repository;
  private final OfferChangeService offerChangeService;
  private final ClientService clients;
  private final ParticipantRuleRegistry participantRules;
  private final CreateReservationRequestValidator createReservationRequestValidator;
  private final List<ReservationUpdateObserver> reservationUpdateObservers;

  @Transactional
  public Reservation create(CreateReservationRequest request) {
    createReservationRequestValidator.validateRequest(request);
    Map<String, Integer> quantities = new LinkedHashMap<>();
    for (var item : request.items()) {
      quantities.merge(item.offerId(), quantity(item), Integer::sum);
    }

    String id = UUID.randomUUID().toString();
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    if (!repository.insertForClient(id, request.clientId(), now)) {
      throw new ClientNotFoundException("Client not found: " + request.clientId());
    }

    Map<String, Offer> offerSnapshots = new LinkedHashMap<>();
    for (var entry : quantities.entrySet()) {
      Offer offer = offerChangeService.reserveQuantity(entry.getKey(), entry.getValue());
      offerSnapshots.put(offer.id(), offer);
    }

    List<OrderItem> items = new ArrayList<>();
    for (var item : request.items()) {
      Offer snapshot = offerSnapshots.get(item.offerId());
      participantRules
          .forMode(snapshot.participantMode())
          .validate(snapshot, quantity(item), item.participants());
      items.add(
          new OrderItem(
              UUID.randomUUID().toString(),
              snapshot,
              quantity(item),
              now,
              item.participants(),
              ReservationStatus.RESERVED));
    }
    repository.storeItems(id, items);
    return new Reservation(id, request.clientId(), items, ReservationStatus.RESERVED, now, now);
  }

  public Reservation findById(String id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + id));
  }

  public List<Reservation> findByClientId(String clientId) {
    clients.findById(clientId);
    return repository.findByClientId(clientId);
  }

  @Transactional
  public Reservation changeItemsStatus(String reservationId, ReservationStatus reservationStatus) {
    // Take SQLite's write lock before reading, so concurrent transitions cannot
    // validate the same stale state or fail while upgrading a read transaction.
    if (!repository.lockForUpdate(reservationId)) {
      throw new ReservationNotFoundException("Reservation not found: " + reservationId);
    }
    Reservation reservation = findById(reservationId);
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    List<OrderItem> updatedItems =
        reservation.items().stream()
            .map(item -> updateItemStatus(reservationStatus, item, now))
            .toList();
    Reservation updatedReservation =
        new Reservation(
            reservation.id(),
            reservation.clientId(),
            updatedItems,
            reservation.status(),
            reservation.createTimestamp(),
            now);
    for (ReservationUpdateObserver observer : reservationUpdateObservers) {
      updatedReservation = observer.handleReservationUpdate(reservation, updatedReservation);
    }
    if (!repository.update(updatedReservation)) {
      throw new IllegalStateException(
          "Could not update order items for reservation " + reservationId);
    }
    return updatedReservation;
  }

  private static OrderItem updateItemStatus(
      ReservationStatus reservationStatus, OrderItem item, Instant now) {
    if (!item.orderItemReservationStatus().canChangeTo(reservationStatus)) {
      throw new RequestConflictException(
          "Item %s status cannot be changed from %s to %s"
              .formatted(item.lineId(), item.orderItemReservationStatus(), reservationStatus));
    }
    return new OrderItem(
        item.lineId(),
        item.offerSnapshot(),
        item.quantity(),
        now,
        item.participants(),
        reservationStatus);
  }

  private static @NonNull Integer quantity(CreateReservationRequest.Item item) {
    return Objects.requireNonNullElse(item.quantity(), 1);
  }
}
