package pl.edu.uj.student.wzorceprojektowe.reservation.client.exception;

import pl.edu.uj.student.wzorceprojektowe.shared.exception.NotFoundException;

public class ClientNotFoundException extends NotFoundException {
  public ClientNotFoundException(String message) {
    super(message);
  }
}
