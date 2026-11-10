package pl.edu.uj.student.wzorceprojektowe.reservation.exception;

import pl.edu.uj.student.wzorceprojektowe.shared.exception.NotFoundException;

public class ReservationNotFoundException extends NotFoundException {
  public ReservationNotFoundException(String message) {
    super(message);
  }
}
