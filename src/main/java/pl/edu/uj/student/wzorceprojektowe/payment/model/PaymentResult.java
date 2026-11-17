package pl.edu.uj.student.wzorceprojektowe.payment.model;

public record PaymentResult(PaymentStatus status, String statusMessage) {

    public boolean successfull() {
        return status == PaymentStatus.SUCCESSFUL;
    }

    public static PaymentResult success() {
        return new PaymentResult(PaymentStatus.SUCCESSFUL, null);
    }
    public static PaymentResult failure(String reason) {
        return new PaymentResult(PaymentStatus.FAILED, reason);
    }

    public static PaymentResult denial(String details) {
        return new PaymentResult(PaymentStatus.DENIED, details);
    }

    public enum PaymentStatus {
        SUCCESSFUL, DENIED, FAILED
    }
}
