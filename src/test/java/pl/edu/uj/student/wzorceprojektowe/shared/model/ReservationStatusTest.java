package pl.edu.uj.student.wzorceprojektowe.shared.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.edu.uj.student.wzorceprojektowe.shared.model.ReservationStatus.*;

class ReservationStatusTest {

  @ParameterizedTest
  @CsvSource({
      "RESERVED, RESERVED, false",
      "RESERVED, PAID, true",
      "RESERVED, CANCELED, true",
      "PAID, RESERVED, false",
      "PAID, PAID, false",
      "PAID, CANCELED, false",
      "CANCELED, RESERVED, false",
      "CANCELED, PAID, false",
      "CANCELED, CANCELED, false"
  })
  void onlyReservedReservationsCanBePaidOrCancelled(
      ReservationStatus from, ReservationStatus to, boolean allowed) {
    assertThat(from.canChangeTo(to)).isEqualTo(allowed);
  }

  @Test
  void terminalStatesHaveNoOutgoingChanges() {
    for (ReservationStatus terminal : new ReservationStatus[]{PAID, CANCELED}) {
      for (ReservationStatus target : ReservationStatus.values()) {
        assertThat(terminal.canChangeTo(target)).isFalse();
      }
    }
    assertThat(RESERVED.canChangeTo(PAID)).isTrue();
  }
}