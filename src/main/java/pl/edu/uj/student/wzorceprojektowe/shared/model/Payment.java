package pl.edu.uj.student.wzorceprojektowe.shared.model;

import java.time.Instant;

public record Payment(
    String id,
    String reservationId,
    String amount,
    String currency,
    PaymentDetails paymentDetails,
    Instant paymentTimestamp) {}
