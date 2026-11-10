package pl.edu.uj.student.wzorceprojektowe.shared.util.validator;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public record ValidationPolicy<T>(Predicate<T> policyCheck, String failureMessage) {
  public boolean isFailing(T offer) {
    return !policyCheck.test(offer);
  }

  public static <T, T2> ValidationPolicy<T> notNullPolicy(
      Function<T, T2> fieldExtractor, String failureMessage) {
    return new ValidationPolicy<>(it -> fieldExtractor.apply(it) != null, failureMessage);
  }

  public static <T> ValidationPolicy<T> notBlankPolicy(
      Function<T, String> fieldExtractor, String failureMessage) {
    return new ValidationPolicy<>(
        it -> StringUtils.hasText(fieldExtractor.apply(it)), failureMessage);
  }

  public static <T> ValidationPolicy<T> maxLengthPolicy(
      Function<T, String> fieldExtractor, int maxLength, String failureMessage) {
    return new ValidationPolicy<>(
        it -> {
          String s = fieldExtractor.apply(it);
          return s == null || s.length() <= maxLength;
        },
        failureMessage);
  }

  public static <T, E> ValidationPolicy<T> notEmptyPolicy(
      Function<T, List<E>> fieldExtractor, String failureMessage) {
    return new ValidationPolicy<>(
        it -> {
          List<E> list = fieldExtractor.apply(it);
          return list != null && !list.isEmpty();
        },
        failureMessage);
  }

  public static <T, E> ValidationPolicy<T> allOfPolicy(
      Function<T, List<E>> fieldExtractor, ValidationPolicy<E> policy) {
    Objects.requireNonNull(policy, "Policy may not be null");
    Objects.requireNonNull(fieldExtractor, "FieldExtractor may not be null");
    return new ValidationPolicy<>(
        it -> {
          List<E> list = fieldExtractor.apply(it);
          return list != null && list.stream().noneMatch(policy::isFailing);
        },
        policy.failureMessage);
  }

  public static <T> ValidationPolicy<T> regexPolicy(
      Function<T, String> fieldExtractor, Pattern regex, String failureMessage) {
    Objects.requireNonNull(regex, "Regex may not be null");
    Objects.requireNonNull(fieldExtractor, "FieldExtractor may not be null");
    return new ValidationPolicy<>(
        it -> {
          String s = fieldExtractor.apply(it);
          return s != null && regex.matcher(s).matches();
        },
        failureMessage);
  }

  public static <T> ValidationPolicy<T> policyOf(Predicate<T> policyCheck, String failureMessage) {
    return new ValidationPolicy<>(policyCheck, failureMessage);
  }
}
