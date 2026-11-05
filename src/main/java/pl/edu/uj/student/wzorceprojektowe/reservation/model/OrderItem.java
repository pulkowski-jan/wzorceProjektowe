package pl.edu.uj.student.wzorceprojektowe.reservation.model;

import java.time.Instant;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Offer;

public record OrderItem(
    String lineId,
    Offer offerSnapshot,
    int quantity,
    Instant timestamp) {}
