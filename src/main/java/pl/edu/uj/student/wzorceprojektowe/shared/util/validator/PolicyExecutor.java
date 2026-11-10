package pl.edu.uj.student.wzorceprojektowe.shared.util.validator;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PolicyExecutor {
  public static <T> void executePolicies(Stream<ValidationPolicy<T>> policies, T validated) {
    String failedPoliciesMessage =
        policies
            .filter(policy -> policy.isFailing(validated))
            .map(ValidationPolicy::failureMessage)
            .collect(Collectors.joining(", "));
    if (StringUtils.hasText(failedPoliciesMessage)) {
      throw new RequestValidationException(failedPoliciesMessage);
    }
  }
}
