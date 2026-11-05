package pl.edu.uj.student.wzorceprojektowe.shared.util.validator;

import java.util.UUID;
import java.util.regex.Pattern;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;

public final class RequestChecks {
  private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

  private RequestChecks() {}

  public static String name(String value) {
    if (value == null || value.isBlank() || value.strip().length() > 255) {
      throw new RequestValidationException("name must contain between 1 and 255 characters");
    }
    return value.strip();
  }

  public static String email(String value) {
    if (value == null || value.strip().length() > 254 || !EMAIL.matcher(value.strip()).matches()) {
      throw new RequestValidationException("email must be a valid email address");
    }
    return value.strip();
  }

  public static void uuid(String value, String field) {
    try {
      if (value == null || value.length() != 36) {
        throw new IllegalArgumentException();
      }
      UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw new RequestValidationException(field + " must be a UUID");
    }
  }
}
