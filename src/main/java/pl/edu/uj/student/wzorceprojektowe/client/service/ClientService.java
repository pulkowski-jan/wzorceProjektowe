package pl.edu.uj.student.wzorceprojektowe.client.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.uj.student.wzorceprojektowe.client.model.Client;
import pl.edu.uj.student.wzorceprojektowe.client.model.ClientRequest;
import pl.edu.uj.student.wzorceprojektowe.client.repository.ClientRepository;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestConflictException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.ResourceNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.shared.util.validator.RequestChecks;

@Service
@RequiredArgsConstructor
public class ClientService {
  private final ClientRepository repository;

  public List<Client> findAll() {
    return repository.findAll();
  }

  public Client findById(String id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
  }

  public Client create(ClientRequest request) {
    return repository.insert(validated(UUID.randomUUID().toString(), request));
  }

  public Client update(String id, ClientRequest request) {
    Client client = validated(id, request);
    if (!repository.update(client)) {
      throw new ResourceNotFoundException("Client not found: " + id);
    }
    return client;
  }

  @Transactional
  public Client delete(String id) {
    return repository.deleteIfUnreferenced(id).orElseGet(() -> {
      findById(id);
      throw new RequestConflictException("A client with reservations cannot be deleted");
    });
  }

  private Client validated(String id, ClientRequest request) {
    if (request == null) {
      throw new RequestValidationException("Client data is required");
    }
    return new Client(id, RequestChecks.name(request.name()), RequestChecks.email(request.email()));
  }
}
