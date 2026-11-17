package pl.edu.uj.student.wzorceprojektowe.reservation.hooks;

import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;

public interface ReservationUpdateObserver {
    Reservation handleReservationUpdate(Reservation oldReservation, Reservation updatedReservation);
}
