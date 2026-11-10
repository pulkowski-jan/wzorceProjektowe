package pl.edu.uj.student.wzorceprojektowe.reservation.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import pl.edu.uj.student.wzorceprojektowe.reservation.model.CreateReservationRequest;
import pl.edu.uj.student.wzorceprojektowe.reservation.service.ReservationService;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.NotFoundException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestConflictException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {
  private final ReservationService service;

  @PostMapping
  public Reservation create(@RequestBody CreateReservationRequest request) {
    return service.create(request);
  }

  @GetMapping("/{id}")
  public Reservation byId(@PathVariable String id) {
    return service.findById(id);
  }

  @GetMapping
  public List<Reservation> byClient(@RequestParam String clientId) {
    return service.findByClientId(clientId);
  }

  @ExceptionHandler(RequestValidationException.class)
  public ErrorResponse handleRequestValidationException(RequestValidationException e) {
    return ErrorResponse.create(e, HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(NotFoundException.class)
  public ErrorResponse handleResourceNotFoundException(RuntimeException e) {
    return ErrorResponse.create(e, HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler({RequestConflictException.class, OptimisticLockingFailureException.class})
  public ErrorResponse handleRequestConflictException(RuntimeException e) {
    return ErrorResponse.create(e, HttpStatus.CONFLICT, e.getMessage());
  }
}
