package pl.edu.uj.student.wzorceprojektowe.client.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pl.edu.uj.student.wzorceprojektowe.client.model.Client;

@Repository
@RequiredArgsConstructor
public class ClientRepository {
  private static final String SELECT_ALL_TEMPLATE =
      "SELECT id, name, email FROM client ORDER BY name, id";
  private static final String SELECT_BY_ID_TEMPLATE =
      "SELECT id, name, email FROM client WHERE id = ?";
  private static final String INSERT_TEMPLATE =
      "INSERT INTO client(id, name, email) VALUES (?, ?, ?)";
  private static final String UPDATE_TEMPLATE =
      "UPDATE client SET name = ?, email = ? WHERE id = ?";
  private static final String DELETE_UNREFERENCED_TEMPLATE = """
      DELETE FROM client WHERE id = ?
      AND NOT EXISTS (SELECT 1 FROM reservation WHERE client_id = client.id)
      RETURNING id, name, email
      """;

  private final JdbcTemplate jdbc;

  public List<Client> findAll() {
    return jdbc.query(SELECT_ALL_TEMPLATE, this::mapClient);
  }

  public Optional<Client> findById(String id) {
    return jdbc.query(SELECT_BY_ID_TEMPLATE, this::mapClient, id)
        .stream().findFirst();
  }

  public Client insert(Client client) {
    jdbc.update(INSERT_TEMPLATE,
        client.id(), client.name(), client.email());
    return client;
  }

  public boolean update(Client client) {
    return jdbc.update(UPDATE_TEMPLATE,
        client.name(), client.email(), client.id()) == 1;
  }

  public Optional<Client> deleteIfUnreferenced(String id) {
    return jdbc.query(DELETE_UNREFERENCED_TEMPLATE, this::mapClient, id)
        .stream().findFirst();
  }

  private Client mapClient(ResultSet rs, int rowNum) throws SQLException {
    return new Client(rs.getString("id"), rs.getString("name"), rs.getString("email"));
  }
}
