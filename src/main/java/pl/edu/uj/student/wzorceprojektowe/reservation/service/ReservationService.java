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
import pl.edu.uj.student.wzorceprojektowe.reservation.model.*;
import pl.edu.uj.student.wzorceprojektowe.reservation.repository.ReservationRepository;
import pl.edu.uj.student.wzorceprojektowe.reservation.util.CreateReservationRequestValidator;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.shared.model.OrderItem;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;

@Service
@RequiredArgsConstructor
public class ReservationService {
  private final ReservationRepository repository;
  private final OfferChangeService offerChangeService;
  private final ClientService clients;
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
      items.add(
          new OrderItem(
              UUID.randomUUID().toString(),
              offerSnapshots.get(item.offerId()),
              quantity(item),
              now));
    }
    repository.storeItems(id, items);
    return new Reservation(id, request.clientId(), items, now, now);
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
