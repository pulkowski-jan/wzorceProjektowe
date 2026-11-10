package pl.edu.uj.student.wzorceprojektowe.shared.util.validator;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PolicyExecutor<T> {
  public void executePolicies(
      Stream<ValidationPolicy<T>> policies,
      T validated,
      Function<String, RuntimeException> exceptionFactory) {
    String failedPoliciesMessage =
        policies
            .filter(policy -> policy.isFailing(validated))
            .map(ValidationPolicy::failureMessage)
            .collect(Collectors.joining(", "));
    if (StringUtils.hasText(failedPoliciesMessage)) {
      throw exceptionFactory.apply(failedPoliciesMessage);
    }
  }
}
