package pl.edu.uj.student.wzorceprojektowe.offer.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
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
      SELECT o.id, o.name, o.currency, o.amount, o.description, o.quantity, o.version,
             v.name AS vendor_name
      FROM offer o JOIN vendor v ON o.vendor_id = v.id
      WHERE o.id = ?
      """;
  private static final String SEARCH_TEMPLATE =
      """
      SELECT o.id, o.name, o.currency, o.amount, o.description, o.quantity, o.version,
             v.name AS vendor_name
      FROM offer o JOIN vendor v ON o.vendor_id = v.id
      WHERE o.name LIKE ? AND v.name LIKE ? AND o.quantity > 0
      """;
  public static final String INSERT_TEMPLATE =
      "INSERT INTO offer(id, name, vendor_id, currency, amount, description, quantity, version) VALUES (?, ?, (SELECT id FROM vendor WHERE name = ?), ?, ?, ?, ?, 1)";
  public static final String UPDATE_TEMPLATE =
      "UPDATE offer SET name = ?, description = ?, amount = ?, currency = ?, quantity = ?, version = ? WHERE id = ? AND version = ?";
  public static final String DELETE_TEMPLATE =
      """
      DELETE FROM offer
      WHERE id = ?
      RETURNING id, name, currency, amount, description, quantity, version,
             (SELECT v.name FROM vendor v WHERE v.id = offer.vendor_id) AS vendor_name
      """;

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
        offer.price().currency(),
        offer.price().getAmountAsString(),
        offer.description(),
        offer.quantity());
    return new Offer(
        offerId,
        offer.name(),
        offer.vendor(),
        offer.price(),
        offer.description(),
        offer.quantity(),
        1);
  }

  public Offer update(Offer offer) {
    int affectedRows = jdbc.update(
        UPDATE_TEMPLATE,
        offer.name(),
        offer.description(),
        offer.price().getAmountAsString(),
        offer.price().currency(),
        offer.quantity(),
        offer.version() + 1,
        offer.id(),
        offer.version());
    if (affectedRows != 1) {
      throw new OptimisticLockingFailureException(
              "Concurrent offer change or was deletion detected for offer: " + offer.id());
    }
    return new Offer(
        offer.id(),
        offer.name(),
        offer.vendor(),
        offer.price(),
        offer.description(),
        offer.quantity(),
        offer.version() + 1);
  }

  public Optional<Offer> deleteById(String offerId) {
    try {
      return Optional.of(jdbc.queryForObject(DELETE_TEMPLATE, this::mapOffer, offerId));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  private Offer mapOffer(ResultSet rs, int rowNum) throws SQLException {
    return new Offer(
        rs.getString("id"),
        rs.getString("name"),
        new Vendor(rs.getString("vendor_name")),
        new Money(rs.getString("currency"), rs.getString("amount")),
        rs.getString("description"),
        rs.getInt("quantity"),
        rs.getInt("version"));
  }
}
