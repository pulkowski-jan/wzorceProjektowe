package pl.edu.uj.student.wzorceprojektowe.shared.model;

public record Offer(
    String id, String name, Vendor vendor, Money price, String description, int quantity, Integer version,
    ParticipantMode participantMode) {

  public Offer {
    participantMode = participantMode == null ? ParticipantMode.NONE : participantMode;
  }

  public Offer(
      String id, String name, Vendor vendor, Money price, String description,
      int quantity, Integer version) {
    this(id, name, vendor, price, description, quantity, version, ParticipantMode.NONE);
  }
}
