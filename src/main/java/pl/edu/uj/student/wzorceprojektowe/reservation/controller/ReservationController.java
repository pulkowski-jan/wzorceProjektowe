package pl.edu.uj.student.wzorceprojektowe.reservation.controller;

import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.uj.student.wzorceprojektowe.reservation.model.CreateReservationRequest;
import pl.edu.uj.student.wzorceprojektowe.reservation.model.Reservation;
import pl.edu.uj.student.wzorceprojektowe.reservation.service.ReservationService;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {
  private final ReservationService service;

  @PostMapping
  public ResponseEntity<Reservation> create(@RequestBody CreateReservationRequest request) {
    Reservation reservation = service.create(request);
    return ResponseEntity.created(URI.create("/reservations/" + reservation.id())).body(reservation);
  }

  @GetMapping("/{id}")
  public Reservation byId(@PathVariable String id) {
    return service.findById(id);
  }

  @GetMapping
  public List<Reservation> byClient(@RequestParam String clientId) {
    return service.findByClientId(clientId);
  }
}
