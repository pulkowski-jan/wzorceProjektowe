package pl.edu.uj.student.wzorceprojektowe.client.controller;

import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.uj.student.wzorceprojektowe.client.model.Client;
import pl.edu.uj.student.wzorceprojektowe.client.model.ClientRequest;
import pl.edu.uj.student.wzorceprojektowe.client.service.ClientService;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {
  private final ClientService service;

  @PostMapping
  public ResponseEntity<Client> create(@RequestBody ClientRequest request) {
    Client client = service.create(request);
    return ResponseEntity.created(URI.create("/clients/" + client.id())).body(client);
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
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
