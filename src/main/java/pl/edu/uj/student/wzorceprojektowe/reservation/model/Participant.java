package pl.edu.uj.student.wzorceprojektowe.reservation.model;

/** Optional attendee data; offer-specific requirements are a separate feature. */
public record Participant(String name, String email) {}
