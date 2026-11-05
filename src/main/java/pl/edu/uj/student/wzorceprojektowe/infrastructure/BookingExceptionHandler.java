package pl.edu.uj.student.wzorceprojektowe.infrastructure;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.edu.uj.student.wzorceprojektowe.client.controller.ClientController;
import pl.edu.uj.student.wzorceprojektowe.offer.exception.OfferNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.reservation.controller.ReservationController;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestConflictException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.ResourceNotFoundException;

@RestControllerAdvice(assignableTypes = {ClientController.class, ReservationController.class})
public class BookingExceptionHandler {
  @ExceptionHandler(RequestValidationException.class)
  public ErrorResponse badRequest(RequestValidationException e) {
    return ErrorResponse.create(e, HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler({ResourceNotFoundException.class, OfferNotFoundException.class})
  public ErrorResponse notFound(RuntimeException e) {
    return ErrorResponse.create(e, HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler({RequestConflictException.class, OptimisticLockingFailureException.class})
  public ErrorResponse conflict(RuntimeException e) {
    return ErrorResponse.create(e, HttpStatus.CONFLICT, e.getMessage());
  }
}
