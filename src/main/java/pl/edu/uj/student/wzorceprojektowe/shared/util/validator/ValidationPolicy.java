package pl.edu.uj.student.wzorceprojektowe.shared.util.validator;

import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.util.StringUtils;

public record ValidationPolicy<T>(Predicate<T> policyCheck, String failureMessage) {
  public boolean isFailing(T offer) {
    return !policyCheck.test(offer);
  }

  public static <T> ValidationPolicy<T> notNullPolicy(
      Function<T, Object> fieldExtractor, String failureMessage) {
    return new ValidationPolicy<>(offer -> fieldExtractor.apply(offer) != null, failureMessage);
  }

  public static <T> ValidationPolicy<T> notBlankPolicy(
      Function<T, String> fieldExtractor, String failureMessage) {
    return new ValidationPolicy<>(
        offer -> StringUtils.hasText(fieldExtractor.apply(offer)), failureMessage);
  }

  public static <T> ValidationPolicy<T> policyOf(Predicate<T> policyCheck, String failureMessage) {
    return new ValidationPolicy<>(policyCheck, failureMessage);
  }
}
