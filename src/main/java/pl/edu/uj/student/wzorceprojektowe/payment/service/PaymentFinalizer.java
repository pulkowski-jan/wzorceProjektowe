package pl.edu.uj.student.wzorceprojektowe.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.uj.student.wzorceprojektowe.payment.repository.PaymentRepository;
import pl.edu.uj.student.wzorceprojektowe.reservation.service.ReservationService;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;
import pl.edu.uj.student.wzorceprojektowe.shared.model.ReservationStatus;

@Service
@RequiredArgsConstructor
public class PaymentFinalizer {
    private final PaymentRepository paymentRepository;
    private final ReservationService reservationService;

    @Transactional
    public void finalize(Payment payment) {
        paymentRepository.insert(payment);
        reservationService.changeStatus(payment.reservationId(), ReservationStatus.PAID);
    }
}
