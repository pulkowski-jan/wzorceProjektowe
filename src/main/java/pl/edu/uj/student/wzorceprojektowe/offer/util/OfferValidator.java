package pl.edu.uj.student.wzorceprojektowe.offer.util;

import static pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy.*;
import static pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy.maxLengthPolicy;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.edu.uj.student.wzorceprojektowe.offer.exception.OfferValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.PolicyExecutor;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy;

@RequiredArgsConstructor
@Component
public class OfferValidator {
  private final PolicyExecutor<Offer> policyExecutor;

  private static final List<ValidationPolicy<Offer>> OFFER_VALIDATION_POLICIES =
      List.of(
          notNullPolicy(Offer::name, "Offer must have name"),
          notNullPolicy(Offer::price, "Offer must have price"),
          notBlankPolicy(Offer::name, "Offer name cannot be empty"),
          maxLengthPolicy(Offer::name, 100, "Offer name cannot be more than 100 characters"),
          maxLengthPolicy(
              Offer::description, 1000, "Offer name cannot be more than 1000 characters"),
          notBlankPolicy(
              offer -> offer.price() == null ? "" : offer.price().currency(),
              "Offer price currency cannot be empty"),
          policyOf(offer -> offer.quantity() >= 0, "Offer quantity cannot be negative"),
          policyOf(
              offer ->
                  offer.price() == null || offer.price().amount().compareTo(BigDecimal.ZERO) >= 0,
              "Offer price cannot be negative"));

  public void validateOffer(Offer offer) {
    validateOffer(offer, List.of());
  }

  public void validateOffer(Offer offer, List<ValidationPolicy<Offer>> additionalPolicies) {
    if (offer == null) {
      throw new OfferValidationException("Offer must not be null");
    }
    policyExecutor.executePolicies(
        Stream.concat(additionalPolicies.stream(), OFFER_VALIDATION_POLICIES.stream()),
        offer,
        OfferValidationException::new);
  }
}
