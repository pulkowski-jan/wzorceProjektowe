package pl.edu.uj.student.wzorceprojektowe.shared.exception;

public class RequestConflictException extends RuntimeException {
  public RequestConflictException(String message) {
    super(message);
  }
}
