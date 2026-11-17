package pl.edu.uj.student.wzorceprojektowe.shared.model;

public record PaymentDetails(
    PaymentMethod paymentMethod,
    CardPaymentDetails cardPayment,
    CashPaymentDetails cashPayment,
    BlikPaymentDetails blikPayment) {}
