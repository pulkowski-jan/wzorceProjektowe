package pl.edu.uj.student.wzorceprojektowe.shared.exception;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
