package pl.edu.uj.student.wzorceprojektowe.reservation.util;

import static pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy.*;

import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.edu.uj.student.wzorceprojektowe.reservation.model.CreateReservationRequest;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.PolicyExecutor;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.ValidationPolicy;

@RequiredArgsConstructor
@Component
public class CreateReservationRequestValidator {
  private final PolicyExecutor<CreateReservationRequest> policyExecutor;

  private static final List<ValidationPolicy<CreateReservationRequest>> VALIDATION_POLICIES =
      List.of(
          notBlankPolicy(CreateReservationRequest::clientId, "clientId must not be blank"),
          notEmptyPolicy(CreateReservationRequest::items, "Items must not be empty"),
          allOfPolicy(
              CreateReservationRequest::items,
              notNullPolicy(Function.identity(), "OfferItems must not be null")),
          allOfPolicy(
              CreateReservationRequest::items,
              notBlankPolicy(
                  CreateReservationRequestValidator::orderItemOfferId,
                  "OfferItem Ids are required")),
          allOfPolicy(
              CreateReservationRequest::items,
              policyOf(
                  item -> item == null || item.quantity() == null || item.quantity() > 0,
                  "OfferItem quantity must be positive")));

  public void validateRequest(CreateReservationRequest request) {
    if (request == null) {
      throw new RequestValidationException("Reservation data is required");
    }
    policyExecutor.executePolicies(
        VALIDATION_POLICIES.stream(), request, RequestValidationException::new);
  }

  private static String orderItemOfferId(CreateReservationRequest.Item item) {
    return item == null ? "" : item.offerId();
  }
}
