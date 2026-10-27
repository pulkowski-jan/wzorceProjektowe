package pl.edu.uj.student.wzorceprojektowe.offer.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.edu.uj.student.wzorceprojektowe.offer.util.OfferValidator.validateOffer;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.edu.uj.student.wzorceprojektowe.offer.exception.OfferValidationException;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Vendor;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy;

class OfferValidatorTest {

  private static final Vendor VENDOR = new Vendor("Sprzedawca");
  private static final Money PRICE = new Money("PLN", "19.99");

  private Offer validOffer() {
    return new Offer("id-1", "Koncert", VENDOR, PRICE, "Bilet na koncert", 5);
  }

  @Nested
  class ValidOffer {

    @Test
    void validOfferPassesValidation() {
      assertDoesNotThrow(() -> validateOffer(validOffer()));
    }

    @Test
    void zeroQuantityAndZeroAmountAreAllowed() {
      Offer offer = new Offer("id-1", "Darmowy", VENDOR, new Money("PLN", "0.00"), "Opis", 0);
      assertDoesNotThrow(() -> validateOffer(offer));
    }
  }

  @Nested
  class NullOffer {

    @Test
    void nullOfferThrowsInsteadOfNpe() {
      OfferValidationException ex =
          assertThrows(OfferValidationException.class, () -> validateOffer(null));
      assertTrue(ex.getMessage().contains("null"), ex.getMessage());
    }
  }

  @Nested
  class Name {

    @Test
    void nullNameThrows() {
      Offer offer = new Offer("id-1", null, VENDOR, PRICE, "Opis", 5);
      assertThrows(OfferValidationException.class, () -> validateOffer(offer));
    }

    @Test
    void blankNameThrows() {
      Offer offer = new Offer("id-1", "", VENDOR, PRICE, "Opis", 5);
      assertThrows(OfferValidationException.class, () -> validateOffer(offer));
    }

    @Test
    void whitespaceOnlyNameThrows() {
      Offer offer = new Offer("id-1", "   ", VENDOR, PRICE, "Opis", 5);
      assertThrows(OfferValidationException.class, () -> validateOffer(offer));
    }
  }

  @Nested
  class Price {

    @Test
    void nullPriceThrowsInsteadOfNpe() {
      Offer offer = new Offer("id-1", "Koncert", VENDOR, null, "Opis", 5);
      assertThrows(OfferValidationException.class, () -> validateOffer(offer));
    }

    @Test
    void priceWithNullCurrencyThrowsInsteadOfNpe() {
      Offer offer = new Offer("id-1", "Koncert", VENDOR, new Money(null, "19.99"), "Opis", 5);
      assertThrows(OfferValidationException.class, () -> validateOffer(offer));
    }

    @Test
    void priceWithBlankCurrencyThrows() {
      Offer offer = new Offer("id-1", "Koncert", VENDOR, new Money("", "19.99"), "Opis", 5);
      assertThrows(OfferValidationException.class, () -> validateOffer(offer));
    }

    @Test
    void negativeAmountThrowsInsteadOfNpe() {
      Offer offer = new Offer("id-1", "Koncert", VENDOR, new Money("PLN", "-5.00"), "Opis", 5);
      assertThrows(OfferValidationException.class, () -> validateOffer(offer));
    }
  }

  @Nested
  class Quantity {

    @Test
    void negativeQuantityThrows() {
      Offer offer = new Offer("id-1", "Koncert", VENDOR, PRICE, "Opis", -1);
      assertThrows(OfferValidationException.class, () -> validateOffer(offer));
    }
  }

  @Nested
  class AdditionalPolicies {

    @Test
    void additionalPolicyIsEvaluated() {
      Offer offer = new Offer("id-1", "Koncert", null, PRICE, "Opis", 5);
      List<ValidationPolicy<Offer>> extra =
          List.of(new ValidationPolicy<>(o -> o.vendor() != null, "Offer must have vendor"));
      assertThrows(OfferValidationException.class, () -> validateOffer(offer, extra));
    }

    @Test
    void failureMessagesAreJoined() {
      Offer offer = new Offer("id-1", null, VENDOR, null, null, -1);
      OfferValidationException ex =
          assertThrows(OfferValidationException.class, () -> validateOffer(offer));
      assertTrue(ex.getMessage().contains("name"), ex.getMessage());
      assertTrue(ex.getMessage().contains("price"), ex.getMessage());
      assertTrue(ex.getMessage().contains("quantity"), ex.getMessage());
    }
  }
}
