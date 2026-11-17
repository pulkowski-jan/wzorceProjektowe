package pl.edu.uj.student.wzorceprojektowe.payment.service.strategy.card.externalgateway;

import java.time.LocalDate;

public record Card(String number, LocalDate expiryDate, String securityCode) {}
