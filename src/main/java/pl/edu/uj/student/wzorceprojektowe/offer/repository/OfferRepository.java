package pl.edu.uj.student.wzorceprojektowe.offer.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Vendor;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;

@Repository
@RequiredArgsConstructor
public class OfferRepository {

  private static final String SELECT_BY_ID_TEMPLATE =
      """
            SELECT o.id, o.name, o.currency, o.amount, o.description, o.quantity,
                   v.name AS vendor_name
            FROM offer o JOIN vendor v ON o.vendor_id = v.id
            WHERE o.id = ?
            """;
  private static final String SEARCH_TEMPLATE =
      """
            SELECT o.id, o.name, o.currency, o.amount, o.description, o.quantity,
                   v.name AS vendor_name
            FROM offer o JOIN vendor v ON o.vendor_id = v.id
            WHERE o.name LIKE ? AND v.name LIKE ? AND o.quantity > 0
            """;
  public static final String INSERT_TEMPLATE =
      "INSERT INTO offer(id, name, vendor_id, currency, amount, description, quantity) VALUES (?, ?, (SELECT id FROM vendor WHERE name = ?), ?, ?, ?, ?)";
  public static final String UPDATE_TEMPLATE =
      "UPDATE offer SET name = ?, description = ?, amount = ?, currency = ?, quantity = ? WHERE id = ?";
  public static final String DELETE_TEMPLATE = "DELETE FROM offer WHERE id = ? RETURNING *";
  public static final String COUNT_QUERY = "SELECT COUNT(*) FROM offer";

  private final JdbcTemplate jdbc;

  public Optional<Offer> findById(String id) {
    try {
      return Optional.ofNullable(jdbc.queryForObject(SELECT_BY_ID_TEMPLATE, this::mapOffer, id));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public List<Offer> search(String name, String vendorName) {
    return jdbc.query(SEARCH_TEMPLATE, this::mapOffer, "%" + name + "%", "%" + vendorName + "%");
  }

  public Offer insert(Offer offer) {
    String offerId = UUID.randomUUID().toString();
    jdbc.update(
        INSERT_TEMPLATE,
        offerId,
        offer.name(),
        offer.vendor().name(),
        offer.price().getCurrency(),
        offer.price().getAmount(),
        offer.description(),
        offer.quantity());
    return new Offer(
        offerId,
        offer.name(),
        offer.vendor(),
        offer.price(),
        offer.description(),
        offer.quantity());
  }

  public Offer update(Offer offer) {
    jdbc.update(
        UPDATE_TEMPLATE,
        offer.name(),
        offer.description(),
        offer.price().getCurrency(),
        offer.price().getAmount(),
        offer.quantity(),
        offer.id());
    return offer;
  }

  public Optional<Offer> deleteById(String offerId) {
    try {
      return Optional.of(jdbc.queryForObject(DELETE_TEMPLATE, this::mapOffer, offerId));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public long count() {
    Long count = jdbc.queryForObject(COUNT_QUERY, Long.class);
    return count == null ? 0 : count;
  }

  private Offer mapOffer(ResultSet rs, int rowNum) throws SQLException {
    return new Offer(
        rs.getString("id"),
        rs.getString("name"),
        new Vendor(rs.getString("vendor_name")),
        new Money(rs.getString("currency"), rs.getString("amount")),
        rs.getString("description"),
        rs.getInt("quantity"));
  }
}
