package pl.edu.uj.student.wzorceprojektowe.offer.service;

import static pl.edu.uj.student.wzorceprojektowe.offer.util.OfferValidator.validateOffer;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.uj.student.wzorceprojektowe.offer.exception.OfferNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.offer.repository.OfferRepository;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;

@Service
@RequiredArgsConstructor
public class OfferChangeService {
  private final OfferRepository offerRepository;

  public Offer updateOffer(Offer offer) {
    Offer existingOffer =
        offerRepository
            .findById(offer.id())
            .orElseThrow(
                () ->
                    new OfferNotFoundException("Offer with id %s not found".formatted(offer.id())));
    if (offer.vendor() != null && !offer.vendor().equals(existingOffer.vendor())) {
      throw new RequestValidationException("Order vendor cannot be changed");
    }
    Offer newOffer = mergeOffers(existingOffer, offer);
    validateOffer(newOffer);
    return offerRepository.update(newOffer);
  }

  public Offer deleteOffer(String offerId) {
    return offerRepository
        .deleteById(offerId)
        .orElseThrow(
            () -> new OfferNotFoundException("Offer with id %s not found".formatted(offerId)));
  }

  private Offer mergeOffers(Offer existingOffer, Offer newOffer) {
    return new Offer(
        existingOffer.id(),
        Objects.requireNonNullElse(newOffer.name(), existingOffer.name()),
        existingOffer.vendor(),
        Objects.requireNonNullElse(newOffer.price(), existingOffer.price()),
        Objects.requireNonNullElse(newOffer.description(), existingOffer.description()),
        newOffer.quantity() < 0 ? existingOffer.quantity() : newOffer.quantity());
  }
}
