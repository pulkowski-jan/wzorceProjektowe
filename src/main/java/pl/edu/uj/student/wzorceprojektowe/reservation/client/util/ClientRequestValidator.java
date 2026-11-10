package pl.edu.uj.student.wzorceprojektowe.reservation.client.util;

import java.util.List;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.edu.uj.student.wzorceprojektowe.reservation.client.model.ClientRequest;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.PolicyExecutor;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy;

@RequiredArgsConstructor
@Component
public class ClientRequestValidator {
  private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w-.+]+@(?:[\\w-]+\\.)+[\\w-]+");

  private static final List<ValidationPolicy<ClientRequest>> VALIDATION_POLICIES =
      List.of(
          ValidationPolicy.notBlankPolicy(ClientRequest::name, "Client name must not be blank"),
          ValidationPolicy.notBlankPolicy(ClientRequest::email, "Client email must not be blank"),
          ValidationPolicy.maxLengthPolicy(
              ClientRequest::name, 256, "Client name length may not exceed 256"),
          ValidationPolicy.maxLengthPolicy(
              ClientRequest::email, 256, "Client email length may not exceed 256"),
          ValidationPolicy.regexPolicy(
              ClientRequest::email, EMAIL_PATTERN, "Malformed client email"));

  private final PolicyExecutor<ClientRequest> policyExecutor;

  public void validateClientRequest(ClientRequest request) {
    if (request == null) {
      throw new RequestValidationException("Client data is required");
    }
    policyExecutor.executePolicies(VALIDATION_POLICIES.stream(), request, RequestValidationException::new);
  }
}
