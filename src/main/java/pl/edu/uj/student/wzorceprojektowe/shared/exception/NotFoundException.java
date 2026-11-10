package pl.edu.uj.student.wzorceprojektowe.shared.exception;

public class NotFoundException extends RuntimeException {
  public NotFoundException(String message) {
    super(message);
  }
}
