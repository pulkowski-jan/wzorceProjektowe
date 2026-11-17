package pl.edu.uj.student.wzorceprojektowe.payment.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.uj.student.wzorceprojektowe.payment.model.PaymentResult;
import pl.edu.uj.student.wzorceprojektowe.payment.service.PaymentFacade;
import pl.edu.uj.student.wzorceprojektowe.payment.util.PaymentValidator;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.NotFoundException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestConflictException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;

@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
  private final PaymentFacade paymentFacade;
  private final PaymentValidator paymentValidator;

  @PostMapping("/pay")
  public ResponseEntity<PaymentResult> pay(@RequestBody Payment request) {
    try {
      if (request == null) {
        throw new RequestValidationException("Payment must not be null");
      }
      Payment payment = new Payment(UUID.randomUUID().toString(), request.reservationId(),
          request.amount(), request.currency(), request.paymentDetails(),
          Instant.now().truncatedTo(ChronoUnit.MILLIS));
      paymentValidator.validatePayment(payment);
      PaymentResult result = paymentFacade.handlePayment(payment);
      HttpStatus status = switch (result.status()) {
        case SUCCESSFUL -> HttpStatus.OK;
        case DENIED -> HttpStatus.UNPROCESSABLE_ENTITY;
        case FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
      };
      return ResponseEntity.status(status).body(result);
    } catch (RequestValidationException e) {
      return failure(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (NotFoundException e) {
      return failure(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (RequestConflictException e) {
      return failure(HttpStatus.CONFLICT, e.getMessage());
    } catch (Exception e) {
      log.error("Payment processing failed", e);
      return failure(HttpStatus.INTERNAL_SERVER_ERROR, "Payment processing failed.");
    }
  }

  private ResponseEntity<PaymentResult> failure(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(PaymentResult.failure(message));
  }
}
