package pl.edu.uj.student.wzorceprojektowe.shared.exception;

public class RequestValidationException extends RuntimeException {
  public RequestValidationException(String message) {
    super(message);
  }
}
