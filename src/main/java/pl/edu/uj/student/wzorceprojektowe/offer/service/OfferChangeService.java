package pl.edu.uj.student.wzorceprojektowe.offer.service;

import static pl.edu.uj.student.wzorceprojektowe.offer.util.OfferValidator.validateOffer;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.uj.student.wzorceprojektowe.offer.exception.OfferNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.offer.model.UpdateOfferRequest;
import pl.edu.uj.student.wzorceprojektowe.offer.repository.OfferRepository;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;

@Service
@RequiredArgsConstructor
public class OfferChangeService {
  private final OfferRepository offerRepository;

  public Offer updateOffer(UpdateOfferRequest request) {
    Offer existingOffer =
        offerRepository
            .findById(request.id())
            .orElseThrow(
                () ->
                    new OfferNotFoundException(
                        "Offer with id %s not found".formatted(request.id())));
    if (request.vendor() != null && !request.vendor().equals(existingOffer.vendor())) {
      throw new RequestValidationException("Order vendor cannot be changed");
    }
    Offer newOffer = mergeOffers(existingOffer, request);
    validateOffer(newOffer);
    return offerRepository.update(newOffer);
  }

  public Offer deleteOffer(String offerId) {
    return offerRepository
        .deleteById(offerId)
        .orElseThrow(
            () -> new OfferNotFoundException("Offer with id %s not found".formatted(offerId)));
  }

  private Offer mergeOffers(Offer existingOffer, UpdateOfferRequest request) {
    return new Offer(
        existingOffer.id(),
        Objects.requireNonNullElse(request.name(), existingOffer.name()),
        Objects.requireNonNullElse(request.vendor(), existingOffer.vendor()),
        Objects.requireNonNullElse(request.price(), existingOffer.price()),
        Objects.requireNonNullElse(request.description(), existingOffer.description()),
        request.quantity() != null ? request.quantity() : existingOffer.quantity());
  }
}
