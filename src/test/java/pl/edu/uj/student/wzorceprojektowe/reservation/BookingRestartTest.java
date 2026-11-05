package pl.edu.uj.student.wzorceprojektowe.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import pl.edu.uj.student.wzorceprojektowe.WzorowySystemBiletowy;
import pl.edu.uj.student.wzorceprojektowe.client.model.Client;
import pl.edu.uj.student.wzorceprojektowe.client.model.ClientRequest;
import pl.edu.uj.student.wzorceprojektowe.client.service.ClientService;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Vendor;
import pl.edu.uj.student.wzorceprojektowe.offer.repository.OfferRepository;
import pl.edu.uj.student.wzorceprojektowe.offer.service.OfferCreateService;
import pl.edu.uj.student.wzorceprojektowe.reservation.model.CreateReservationRequest;
import pl.edu.uj.student.wzorceprojektowe.reservation.model.Reservation;
import pl.edu.uj.student.wzorceprojektowe.reservation.service.ReservationService;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;

class BookingRestartTest {
  @TempDir Path directory;

  @Test
  void clientAndReservationSurviveAFullContextRestartWithInitializationDisabled() {
    Client client;
    Offer offer;
    Reservation reservation;
    try (var first = start("always")) {
      client = first.getBean(ClientService.class).create(new ClientRequest("Buyer", "buyer@example.org"));
      offer = first.getBean(OfferCreateService.class).create(
          new Offer(null, "Concert", new Vendor("Vendor"), new Money("PLN", "12.34"), "", 3, null));
      reservation = first.getBean(ReservationService.class).create(
          new CreateReservationRequest(client.id(),
              List.of(new CreateReservationRequest.Item(offer.id(), 2, null))));
    }
    try (var restarted = start("never")) {
      assertThat(restarted.getBean(ClientService.class).findById(client.id())).isEqualTo(client);
      assertThat(restarted.getBean(ReservationService.class).findById(reservation.id()))
          .isEqualTo(reservation);
      assertThat(restarted.getBean(OfferRepository.class).findById(offer.id()).orElseThrow().quantity())
          .isEqualTo(1);
    }
  }

  private ConfigurableApplicationContext start(String initialization) {
    return new SpringApplicationBuilder(WzorowySystemBiletowy.class)
        .web(WebApplicationType.NONE)
        .run("--spring.datasource.url=jdbc:sqlite:" + directory.resolve("booking.db"),
            "--spring.datasource.hikari.connection-init-sql=PRAGMA foreign_keys = ON",
            "--spring.sql.init.mode=" + initialization,
            "--spring.main.banner-mode=off");
  }
}
