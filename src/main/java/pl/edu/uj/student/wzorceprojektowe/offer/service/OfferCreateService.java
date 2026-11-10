package pl.edu.uj.student.wzorceprojektowe.offer.service;

import static pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy.notBlankPolicy;
import static pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy.notNullPolicy;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.uj.student.wzorceprojektowe.offer.repository.OfferRepository;
import pl.edu.uj.student.wzorceprojektowe.offer.util.OfferValidator;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Vendor;

@Service
@RequiredArgsConstructor
public class OfferCreateService {
  private final OfferRepository offerRepository;
  private final VendorService vendorService;
  private final OfferValidator offerValidator;

  public Offer create(Offer newOffer) {
    offerValidator.validateOffer(
        newOffer,
        List.of(
            notNullPolicy(Offer::vendor, "Offer must have vendor"),
            notBlankPolicy(
                offer -> offer.vendor() == null ? "" : offer.vendor().name(),
                "Offer vendor name cannot be empty")));
    Vendor vendor = vendorService.findOrCreate(newOffer.vendor().name());
    return offerRepository.insert(
        new Offer(
            "",
            newOffer.name(),
            vendor,
            newOffer.price(),
            Objects.requireNonNullElse(newOffer.description(), ""),
            newOffer.quantity(),
            null));
  }
}
