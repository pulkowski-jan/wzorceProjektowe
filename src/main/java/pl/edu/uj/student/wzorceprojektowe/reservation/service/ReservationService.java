package pl.edu.uj.student.wzorceprojektowe.reservation.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.uj.student.wzorceprojektowe.client.service.ClientService;
import pl.edu.uj.student.wzorceprojektowe.offer.exception.OfferNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.offer.repository.OfferRepository;
import pl.edu.uj.student.wzorceprojektowe.reservation.model.*;
import pl.edu.uj.student.wzorceprojektowe.reservation.repository.ReservationRepository;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestConflictException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.ResourceNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.RequestChecks;

@Service
@RequiredArgsConstructor
public class ReservationService {
  private final ReservationRepository repository;
  private final OfferRepository offers;
  private final ClientService clients;

  @Transactional
  public Reservation create(CreateReservationRequest request) {
    validate(request);
    String id = UUID.randomUUID().toString();
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    if (!repository.insertForClient(id, request.clientId(), now)) {
      throw new ResourceNotFoundException("Client not found: " + request.clientId());
    }

    // Aggregate repeated offers to prevent overbooking across lines and preserve one snapshot.
    Map<String, Integer> quantities = new LinkedHashMap<>();
    for (var item : request.items()) {
      try {
        quantities.merge(item.offerId(), item.quantity(), Math::addExact);
      } catch (ArithmeticException e) {
        throw new RequestValidationException("Total quantity exceeds the supported integer range");
      }
    }
    Map<String, Offer> snapshots = new LinkedHashMap<>();
    for (var entry : quantities.entrySet()) {
      Offer offer = offers.findById(entry.getKey())
          .orElseThrow(() -> new OfferNotFoundException("Offer not found: " + entry.getKey()));
      if (!offers.reserveQuantity(offer.id(), entry.getValue(), offer.version())) {
        throw new RequestConflictException("Insufficient inventory or concurrent change: " + offer.id());
      }
      snapshots.put(offer.id(), offer);
    }

    List<OrderItem> items = new ArrayList<>();
    for (var item : request.items()) {
      items.add(new OrderItem(UUID.randomUUID().toString(), snapshots.get(item.offerId()),
          item.quantity(), now, ReservationStatus.RESERVED, normalized(item.participant())));
    }
    repository.storeItems(id, items);
    return new Reservation(id, request.clientId(), items, now, now);
  }

  public Reservation findById(String id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
  }

  public List<Reservation> findByClientId(String clientId) {
    clients.findById(clientId);
    return repository.findByClientId(clientId);
  }

  private void validate(CreateReservationRequest request) {
    if (request == null) {
      throw new RequestValidationException("Reservation data is required");
    }
    RequestChecks.uuid(request.clientId(), "clientId");
    if (request.items() == null || request.items().isEmpty()) {
      throw new RequestValidationException("Reservation must contain at least one item");
    }
    for (var item : request.items()) {
      if (item == null || item.quantity() == null || item.quantity() <= 0) {
        throw new RequestValidationException("Each item must have a positive integer quantity");
      }
      RequestChecks.uuid(item.offerId(), "offerId");
      normalized(item.participant());
    }
  }

  private Participant normalized(Participant participant) {
    if (participant == null) {
      return null;
    }
    return new Participant(RequestChecks.name(participant.name()),
        participant.email() == null ? null : RequestChecks.email(participant.email()));
  }
}
