package pl.edu.uj.student.wzorceprojektowe.offer.exception;

public class VendorNotFoundException extends RuntimeException {
  public VendorNotFoundException(String message) {
    super(message);
  }
}
