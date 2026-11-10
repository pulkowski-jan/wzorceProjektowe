package pl.edu.uj.student.wzorceprojektowe.shared.model;

import java.time.Instant;

public record OrderItem(
    String lineId,
    Offer offerSnapshot,
    int quantity,
    Instant timestamp) {}
