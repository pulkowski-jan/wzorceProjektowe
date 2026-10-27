package pl.edu.uj.student.wzorceprojektowe.offer.exception;

import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;

public class OfferValidationException extends RequestValidationException {
    public OfferValidationException(String message) {
        super(message);
    }
}
