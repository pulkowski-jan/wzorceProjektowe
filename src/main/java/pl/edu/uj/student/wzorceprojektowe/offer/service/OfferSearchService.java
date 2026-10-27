package pl.edu.uj.student.wzorceprojektowe.offer.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.uj.student.wzorceprojektowe.offer.exception.OfferNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.offer.repository.OfferRepository;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;

@Service
@RequiredArgsConstructor
public class OfferSearchService {
  private final OfferRepository offerRepository;

  public Offer searchById(String offerId) {
    if (offerId.isBlank()) {
      throw new RequestValidationException("Offer id cannot be blank");
    }
    return offerRepository
        .findById(offerId)
        .orElseThrow(
            () -> new OfferNotFoundException("Order of id %s does not exist".formatted(offerId)));
  }

  public List<Offer> searchByNameAndVendor(String offerName, String vendorName) {
    if (offerName.isBlank() && vendorName.isBlank()) {
      throw new RequestValidationException("Both offer and vendor name cannot be blank.");
    }
    return offerRepository.search(offerName, vendorName);
  }
}
