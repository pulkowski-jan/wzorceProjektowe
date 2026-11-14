package pl.edu.uj.student.wzorceprojektowe.shared.model;

public record Participant(String name) {
  public Participant {
    name = name == null ? null : name.strip();
  }
}
