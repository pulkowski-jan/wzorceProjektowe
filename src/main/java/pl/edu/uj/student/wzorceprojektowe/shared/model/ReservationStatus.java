package pl.edu.uj.student.wzorceprojektowe.shared.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum ReservationStatus {
  RESERVED,
  PAID,
  CANCELED;

  public boolean canChangeTo(ReservationStatus target) {
    return allowedTargets().contains(target);
  }

  private Set<ReservationStatus> allowedTargets() {
    return switch (this) {
      case RESERVED -> Set.of(PAID, CANCELED);
      case PAID, CANCELED -> Set.of();
    };
  }

  public static Set<ReservationStatus> predecessorsOf(ReservationStatus target) {
    return Arrays.stream(values())
        .filter(status -> status.canChangeTo(target))
        .collect(Collectors.toUnmodifiableSet());
  }
}
