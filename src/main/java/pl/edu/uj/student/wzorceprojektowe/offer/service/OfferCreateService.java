package pl.edu.uj.student.wzorceprojektowe.offer.service;

import static pl.edu.uj.student.wzorceprojektowe.offer.util.OfferValidator.validateOffer;
import static pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy.notBlankPolicy;
import static pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy.notNullPolicy;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Vendor;
import pl.edu.uj.student.wzorceprojektowe.offer.repository.OfferRepository;

@Service
@RequiredArgsConstructor
public class OfferCreateService {
  private final OfferRepository offerRepository;
  private final VendorService vendorService;

  public Offer create(Offer newOffer) {
    validateOffer(
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
            newOffer.quantity()));
  }
}
