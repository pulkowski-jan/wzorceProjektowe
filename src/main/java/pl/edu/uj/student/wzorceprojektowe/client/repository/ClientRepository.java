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
  private final JdbcTemplate jdbc;

  public List<Client> findAll() {
    return jdbc.query("SELECT id, name, email FROM client ORDER BY name, id", this::mapClient);
  }

  public Optional<Client> findById(String id) {
    return jdbc.query("SELECT id, name, email FROM client WHERE id = ?", this::mapClient, id)
        .stream().findFirst();
  }

  public Client insert(Client client) {
    jdbc.update("INSERT INTO client(id, name, email) VALUES (?, ?, ?)",
        client.id(), client.name(), client.email());
    return client;
  }

  public boolean update(Client client) {
    return jdbc.update("UPDATE client SET name = ?, email = ? WHERE id = ?",
        client.name(), client.email(), client.id()) == 1;
  }

  public boolean deleteIfUnreferenced(String id) {
    return jdbc.update("""
        DELETE FROM client WHERE id = ?
        AND NOT EXISTS (SELECT 1 FROM reservation WHERE client_id = client.id)
        """, id) == 1;
  }

  private Client mapClient(ResultSet rs, int rowNum) throws SQLException {
    return new Client(rs.getString("id"), rs.getString("name"), rs.getString("email"));
  }
}
