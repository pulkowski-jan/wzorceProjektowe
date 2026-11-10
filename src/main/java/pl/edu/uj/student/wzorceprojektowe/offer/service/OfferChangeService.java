package pl.edu.uj.student.wzorceprojektowe.offer.service;



import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.uj.student.wzorceprojektowe.offer.exception.OfferNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.offer.util.OfferValidator;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.offer.model.UpdateOfferRequest;
import pl.edu.uj.student.wzorceprojektowe.offer.repository.OfferRepository;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestConflictException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;

@Service
@RequiredArgsConstructor
public class OfferChangeService {
  private final OfferRepository offerRepository;
  private final OfferValidator offerValidator;

  public Offer updateOffer(UpdateOfferRequest request) {
    Offer existingOffer =
        offerRepository
            .findById(request.id())
            .orElseThrow(() ->
                new OfferNotFoundException("Offer with id %s not found".formatted(request.id())));
    if (request.vendor() != null && !request.vendor().equals(existingOffer.vendor())) {
      throw new RequestValidationException("Order vendor cannot be changed");
    }
    Offer newOffer = mergeOffers(existingOffer, request);
    offerValidator.validateOffer(newOffer);
    return offerRepository.update(newOffer);
  }

  public Offer deleteOffer(String offerId) {
    return offerRepository
        .deleteById(offerId)
        .orElseThrow(
            () -> new OfferNotFoundException("Offer with id %s not found".formatted(offerId)));
  }

  public Offer reserveQuantity(String offerId, int quantity) {
    if (quantity <= 0) {
      throw new RequestValidationException("Reserved quantity must be positive");
    }
    Offer offer = offerRepository.findById(offerId)
        .orElseThrow(() -> new OfferNotFoundException("Offer not found: " + offerId));
    if (quantity > offer.quantity()) {
      throw new RequestConflictException("Insufficient inventory: " + offerId);
    }
    Offer updated = new Offer(
        offer.id(), offer.name(), offer.vendor(), offer.price(), offer.description(),
        offer.quantity() - quantity, offer.version());
    offerValidator.validateOffer(updated);
    offerRepository.update(updated);
    return offer;
  }

  private Offer mergeOffers(Offer existingOffer, UpdateOfferRequest request) {
    return new Offer(
        existingOffer.id(),
        Objects.requireNonNullElse(request.name(), existingOffer.name()),
        Objects.requireNonNullElse(request.vendor(), existingOffer.vendor()),
        Objects.requireNonNullElse(request.price(), existingOffer.price()),
        Objects.requireNonNullElse(request.description(), existingOffer.description()),
        request.quantity() != null ? request.quantity() : existingOffer.quantity(),
        existingOffer.version());
  }
}
