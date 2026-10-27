package pl.edu.uj.student.wzorceprojektowe.offer.util;

import static pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;
import pl.edu.uj.student.wzorceprojektowe.offer.exception.OfferValidationException;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OfferValidator {

  private static final List<ValidationPolicy<Offer>> OFFER_VALIDATION_POLICIES =
      List.of(
          notNullPolicy(Offer::name, "Offer must have name"),
          notNullPolicy(Offer::price, "Offer must have price"),
          notBlankPolicy(Offer::name, "Offer name cannot be empty"),
          notBlankPolicy(
              offer -> offer.price().getCurrency(), "Offer price currency cannot be empty"),
          policyOf(offer -> offer.quantity() >= 0, "Offer quantity cannot be negative"),
          policyOf(offer -> offer.price().getAmount() >= 0, "Offer price cannot be negative"));

  public static void validateOffer(Offer offer) {
    validateOffer(offer, List.of());
  }

  public static void validateOffer(Offer offer, List<ValidationPolicy<Offer>> additionalPolicies) {
    String failedPoliciesMessage =
        Stream.concat(additionalPolicies.stream(), OFFER_VALIDATION_POLICIES.stream())
            .filter(policy -> policy.isFailing(offer))
            .map(ValidationPolicy::failureMessage)
            .collect(Collectors.joining(", "));
    if (StringUtils.hasText(failedPoliciesMessage)) {
      throw new OfferValidationException(failedPoliciesMessage);
    }
  }
}
