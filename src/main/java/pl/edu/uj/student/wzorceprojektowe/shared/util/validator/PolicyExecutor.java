package pl.edu.uj.student.wzorceprojektowe.shared.util.validator;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class PolicyExecutor<T> {
  public void executePolicies(
      Stream<ValidationPolicy<T>> policies,
      T validated,
      Function<String, RuntimeException> exceptionFactory) {
    List<ValidationResult> result =
        policies
            .map(policy -> executePolicy(policy, validated))
            .filter(ValidationResult::failed)
            .toList();
    if (!result.isEmpty()) {
      String failureMessage =
          result.stream()
              .map(ValidationResult::message)
              .filter(Objects::nonNull)
              .collect(Collectors.joining(", "));
      throw exceptionFactory.apply(failureMessage);
    }
  }

  private ValidationResult executePolicy(ValidationPolicy<T> policy, T validated) {
    if (policy.isFailing(validated)) {
      return new ValidationResult(true, policy.failureMessage());
    }
    return ValidationResult.SUCCESS;
  }

  private record ValidationResult(boolean failed, String message) {
    private static final ValidationResult SUCCESS = new ValidationResult(false, null);
  }
}
