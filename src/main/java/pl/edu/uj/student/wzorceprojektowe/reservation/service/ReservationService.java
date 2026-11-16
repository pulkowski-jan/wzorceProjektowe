package pl.edu.uj.student.wzorceprojektowe.reservation.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.uj.student.wzorceprojektowe.offer.service.OfferChangeService;
import pl.edu.uj.student.wzorceprojektowe.reservation.client.exception.ClientNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.reservation.client.service.ClientService;
import pl.edu.uj.student.wzorceprojektowe.reservation.exception.ReservationNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.reservation.model.CreateReservationRequest;
import pl.edu.uj.student.wzorceprojektowe.reservation.participant.ParticipantRuleRegistry;
import pl.edu.uj.student.wzorceprojektowe.reservation.repository.ReservationRepository;
import pl.edu.uj.student.wzorceprojektowe.reservation.util.CreateReservationRequestValidator;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestConflictException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.shared.model.OrderItem;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;
import pl.edu.uj.student.wzorceprojektowe.shared.model.ReservationStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReservationService {
  private final ReservationRepository repository;
  private final OfferChangeService offerChangeService;
  private final ClientService clients;
  private final ParticipantRuleRegistry participantRules;
  private final CreateReservationRequestValidator createReservationRequestValidator;

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
      participantRules.
          forMode(snapshot.participantMode()).
          validate(snapshot, quantity(item), item.participants());
      items.add(
          new OrderItem(
              UUID.randomUUID().toString(),
              snapshot,
              quantity(item),
              now,
              item.participants()));
    }
    repository.storeItems(id, items);
    return new Reservation(id, request.clientId(), items, ReservationStatus.RESERVED, now, now);
  }

  @Transactional
  public Reservation changeStatus(String id, ReservationStatus target) {
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    if (!repository.updateStatus(id, ReservationStatus.predecessorsOf(target), target, now)) {
      Reservation current = findById(id);
      throw new RequestConflictException(
          "Reservation status cannot be changed from %s to %s".formatted(current.status(), target));
    }
    Reservation updated = findById(id);
    if (target == ReservationStatus.CANCELED) {
      releaseStock(updated);
    }
    return updated;
  }

  private void releaseStock(Reservation reservation) {
    Map<String, Integer> quantities = new LinkedHashMap<>();
    for (OrderItem item : reservation.items()) {
      quantities.merge(item.offerSnapshot().id(), item.quantity(), Integer::sum);
    }
    quantities.forEach(offerChangeService::releaseQuantity);
    return findById(id);
  }

  private static @NonNull Integer quantity(CreateReservationRequest.Item item) {
    return Objects.requireNonNullElse(item.quantity(), 1);
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
}
