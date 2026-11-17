package pl.edu.uj.student.wzorceprojektowe.payment.util;

import static pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.BlikPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.CardPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.CashPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;
import pl.edu.uj.student.wzorceprojektowe.shared.model.PaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.PolicyExecutor;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy;

@RequiredArgsConstructor
@Component
public class PaymentValidator {
  private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w-.+]+@(?:[\\w-]+\\.)+[\\w-]+");
  private static final Pattern SECURITY_CODE_PATTERN = Pattern.compile("\\d{3,4}");
  private static final Pattern BLIK_CODE_PATTERN = Pattern.compile("\\d{6}");

  private static final List<ValidationPolicy<Payment>> PAYMENT_POLICIES =
      List.of(
          notBlankPolicy(Payment::reservationId, "Payment reservationId cannot be empty"),
          notBlankPolicy(Payment::amount, "Payment amount cannot be empty"),
          policyOf(
              payment -> isNonNegativeDecimal(payment.amount()),
              "Payment amount must be a non-negative decimal number"),
          notBlankPolicy(Payment::currency, "Payment currency cannot be empty"),
          notNullPolicy(Payment::paymentDetails, "Payment must have payment details"),
          notNullPolicy(Payment::paymentTimestamp, "Payment must have payment timestamp"));

  private static final List<ValidationPolicy<Payment>> PAYMENT_DETAILS_POLICIES =
      List.of(
          notNullPolicy(
              payment -> detailsOf(payment) == null ? null : detailsOf(payment).paymentMethod(),
              "Payment details must have payment method"),
          policyOf(
              PaymentValidator::hasExactlyOneMatchingDetail,
              "Payment details must contain exactly one detail matching the payment method"));

  private static final List<ValidationPolicy<Payment>> CARD_PAYMENT_POLICIES =
      List.of(
          notNullPolicy(PaymentValidator::card, "Card payment must have card details"),
          notBlankPolicy(PaymentValidator::cardNumber, "Card number cannot be empty"),
          regexPolicy(
              PaymentValidator::securityCode,
              SECURITY_CODE_PATTERN,
              "Card security code must consist of 3 or 4 digits"),
          policyOf(
              payment -> {
                Integer expirationMonth = expirationMonth(payment);
                return expirationMonth != null && expirationMonth >= 1 && expirationMonth <= 12;
              },
              "Card expiration month must be between 1 and 12"),
          policyOf(
              payment -> {
                Integer expirationYear = expirationYear(payment);
                return expirationYear != null && expirationYear > 1900;
              },
              "Card expiration year must be greater than 1900"),
          notBlankPolicy(PaymentValidator::cardName, "Card owner name cannot be empty"));

  private static final List<ValidationPolicy<Payment>> CASH_PAYMENT_POLICIES =
      List.of(
          notNullPolicy(PaymentValidator::cash, "Cash payment must have cash details"),
          notBlankPolicy(PaymentValidator::agentCode, "Agent code cannot be empty"));

  private static final List<ValidationPolicy<Payment>> BLIK_PAYMENT_POLICIES =
      List.of(
          notNullPolicy(PaymentValidator::blik, "BLIK payment must have BLIK details"),
          regexPolicy(
              PaymentValidator::blikCode, BLIK_CODE_PATTERN, "BLIK code must consist of 6 digits"),
          regexPolicy(PaymentValidator::blikEmail, EMAIL_PATTERN, "Malformed BLIK email"));

  private final PolicyExecutor<Payment> policyExecutor;

  public void validatePayment(Payment payment) {
    validatePayment(payment, List.of());
  }

  public void validatePayment(Payment payment, List<ValidationPolicy<Payment>> additionalPolicies) {
    if (payment == null) {
      throw new RequestValidationException("Payment must not be null");
    }
    policyExecutor.executePolicies(
        Stream.concat(
            Stream.concat(additionalPolicies.stream(), PAYMENT_POLICIES.stream()),
            PAYMENT_DETAILS_POLICIES.stream()),
        payment,
        RequestValidationException::new);

    PaymentDetails details = payment.paymentDetails();
    if (details == null || details.paymentMethod() == null) {
      return;
    }
    List<ValidationPolicy<Payment>> methodPolicies =
        switch (details.paymentMethod()) {
          case CARD -> CARD_PAYMENT_POLICIES;
          case CASH -> CASH_PAYMENT_POLICIES;
          case BLIK -> BLIK_PAYMENT_POLICIES;
        };
    policyExecutor.executePolicies(
        methodPolicies.stream(), payment, RequestValidationException::new);
  }

  private static boolean hasExactlyOneMatchingDetail(Payment payment) {
    PaymentDetails details = detailsOf(payment);
    if (details == null || details.paymentMethod() == null) {
      return false;
    }

    if (countSuppliedPaymentMethodDetails(details) != 1) {
      return false;
    }

    return switch (details.paymentMethod()) {
      case CARD -> details.cardPayment() != null;
      case CASH -> details.cashPayment() != null;
      case BLIK -> details.blikPayment() != null;
    };
  }

  private static long countSuppliedPaymentMethodDetails(PaymentDetails details) {
    return Stream.of(details.cardPayment(), details.cashPayment(), details.blikPayment())
            .filter(Objects::nonNull)
            .count();
  }

  private static boolean isNonNegativeDecimal(String amount) {
    if (amount == null) {
      return false;
    }
    try {
      return new BigDecimal(amount).compareTo(BigDecimal.ZERO) >= 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static PaymentDetails detailsOf(Payment payment) {
    return payment == null ? null : payment.paymentDetails();
  }

  private static CardPaymentDetails card(Payment payment) {
    PaymentDetails details = detailsOf(payment);
    return details == null ? null : details.cardPayment();
  }

  private static CashPaymentDetails cash(Payment payment) {
    PaymentDetails details = detailsOf(payment);
    return details == null ? null : details.cashPayment();
  }

  private static BlikPaymentDetails blik(Payment payment) {
    PaymentDetails details = detailsOf(payment);
    return details == null ? null : details.blikPayment();
  }

  private static String cardNumber(Payment payment) {
    CardPaymentDetails card = card(payment);
    return card == null ? null : card.cardNumber();
  }

  private static String securityCode(Payment payment) {
    CardPaymentDetails card = card(payment);
    return card == null ? null : card.securityCode();
  }

  private static Integer expirationMonth(Payment payment) {
    CardPaymentDetails card = card(payment);
    return card == null ? null : card.expirationMonth();
  }

  private static Integer expirationYear(Payment payment) {
    CardPaymentDetails card = card(payment);
    return card == null ? null : card.expirationYear();
  }

  private static String cardName(Payment payment) {
    CardPaymentDetails card = card(payment);
    return card == null ? null : card.name();
  }

  private static String agentCode(Payment payment) {
    CashPaymentDetails cash = cash(payment);
    return cash == null ? null : cash.agentCode();
  }

  private static String blikCode(Payment payment) {
    BlikPaymentDetails blik = blik(payment);
    return blik == null ? null : blik.blikCode();
  }

  private static String blikEmail(Payment payment) {
    BlikPaymentDetails blik = blik(payment);
    return blik == null ? null : blik.email();
  }
}
