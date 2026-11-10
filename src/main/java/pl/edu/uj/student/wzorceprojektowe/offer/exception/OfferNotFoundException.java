package pl.edu.uj.student.wzorceprojektowe.offer.exception;

import pl.edu.uj.student.wzorceprojektowe.shared.exception.NotFoundException;

public class OfferNotFoundException extends NotFoundException {
  public OfferNotFoundException(String message) {
    super(message);
  }
}
