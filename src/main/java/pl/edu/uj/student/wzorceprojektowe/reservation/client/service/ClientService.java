package pl.edu.uj.student.wzorceprojektowe.reservation.client.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.uj.student.wzorceprojektowe.reservation.client.ClientRequestValidator;
import pl.edu.uj.student.wzorceprojektowe.reservation.client.exception.ClientNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.reservation.client.model.ClientRequest;
import pl.edu.uj.student.wzorceprojektowe.reservation.client.repository.ClientRepository;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestConflictException;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Client;

@Service
@RequiredArgsConstructor
public class ClientService {
  private final ClientRepository repository;

  public List<Client> findAll() {
    return repository.findAll();
  }

  public Client findById(String id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ClientNotFoundException("Client not found: " + id));
  }

  public Client create(ClientRequest request) {
    return repository.insert(client(UUID.randomUUID().toString(), request));
  }

  public Client update(String id, ClientRequest request) {
    Client client = client(id, request);
    if (!repository.update(client)) {
      throw new ClientNotFoundException("Client not found: " + id);
    }
    return client;
  }

  @Transactional
  public Client delete(String id) {
    return repository
        .deleteIfUnreferenced(id)
        .orElseGet(() -> {
          findById(id);
          throw new RequestConflictException("A client with reservations cannot be deleted");
        });
  }

  private Client client(String id, ClientRequest request) {
    ClientRequestValidator.validateClientRequest(request);
    return new Client(id, request.name().strip(), request.email().strip());
  }
}
