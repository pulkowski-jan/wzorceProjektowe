package pl.edu.uj.student.wzorceprojektowe.reservation.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pl.edu.uj.student.wzorceprojektowe.shared.model.OrderItem;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Reservation;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
@RequiredArgsConstructor
public class ReservationRepository {
  private static final String INSERT_FOR_CLIENT_TEMPLATE = """
      INSERT INTO reservation
        (id, version, client_id, offer_items_data, create_timestamp, last_modified_timestamp)
      SELECT ?, 1, id, '[]', ?, ? FROM client WHERE id = ?
      """;
  private static final String UPDATE_ITEMS_TEMPLATE =
      "UPDATE reservation SET offer_items_data = ? WHERE id = ?";
  private static final String SELECT_BY_ID_TEMPLATE =
      "SELECT * FROM reservation WHERE id = ?";
  private static final String SELECT_BY_CLIENT_ID_TEMPLATE =
      "SELECT * FROM reservation WHERE client_id = ? ORDER BY create_timestamp, id";
  private static final TypeReference<List<OrderItem>> ITEMS_TYPE = new TypeReference<>() {};
  private final JdbcTemplate jdbc;
  private final JsonMapper mapper;

  public boolean insertForClient(String id, String clientId, Instant timestamp) {
    return jdbc.update(INSERT_FOR_CLIENT_TEMPLATE,
        id, timestamp.toEpochMilli(), timestamp.toEpochMilli(), clientId) == 1;
  }

  public void storeItems(String id, List<OrderItem> items) {
    int updated = jdbc.update(UPDATE_ITEMS_TEMPLATE,
        mapper.writeValueAsString(items), id);
    if (updated != 1) {
      throw new IllegalStateException("Reservation disappeared during creation: " + id);
    }
  }

  public Optional<Reservation> findById(String id) {
    return jdbc.query(SELECT_BY_ID_TEMPLATE, this::mapReservation, id)
        .stream().findFirst();
  }

  public List<Reservation> findByClientId(String clientId) {
    return jdbc.query(SELECT_BY_CLIENT_ID_TEMPLATE, this::mapReservation, clientId);
  }

  private Reservation mapReservation(ResultSet rs, int rowNum) throws SQLException {
    return new Reservation(
        rs.getString("id"),
        rs.getString("client_id"),
        mapper.readValue(rs.getString("offer_items_data"), ITEMS_TYPE),
        Instant.ofEpochMilli(rs.getLong("create_timestamp")),
        Instant.ofEpochMilli(rs.getLong("last_modified_timestamp")));
  }
}
