package pl.edu.uj.student.wzorceprojektowe.reservation.client.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Client;
import pl.edu.uj.student.wzorceprojektowe.reservation.client.model.ClientRequest;
import pl.edu.uj.student.wzorceprojektowe.reservation.client.service.ClientService;
import pl.edu.uj.student.wzorceprojektowe.reservation.client.exception.ClientNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestConflictException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {
  private final ClientService service;

  @PostMapping
  public Client create(@RequestBody ClientRequest request) {
    return service.create(request);
  }

  @GetMapping
  public List<Client> list() {
    return service.findAll();
  }

  @GetMapping("/{id}")
  public Client byId(@PathVariable String id) {
    return service.findById(id);
  }

  @PutMapping("/{id}")
  public Client update(@PathVariable String id, @RequestBody ClientRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  public Client delete(@PathVariable String id) {
    return service.delete(id);
  }

  @ExceptionHandler(RequestValidationException.class)
  public ErrorResponse handleRequestValidationException(RequestValidationException e) {
    return ErrorResponse.create(e, HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(ClientNotFoundException.class)
  public ErrorResponse handleResourceNotFoundException(ClientNotFoundException e) {
    return ErrorResponse.create(e, HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(RequestConflictException.class)
  public ErrorResponse handleRequestConflictException(RequestConflictException e) {
    return ErrorResponse.create(e, HttpStatus.CONFLICT, e.getMessage());
  }
}
