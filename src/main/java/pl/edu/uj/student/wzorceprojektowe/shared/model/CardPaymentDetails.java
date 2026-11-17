package pl.edu.uj.student.wzorceprojektowe.shared.model;

public record CardPaymentDetails(
    String cardNumber,
    String securityCode,
    Integer expirationMonth,
    Integer expirationYear,
    String name,
    String billingAddress) {}
