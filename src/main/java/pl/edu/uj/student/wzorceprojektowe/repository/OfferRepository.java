package pl.edu.uj.student.wzorceprojektowe.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pl.edu.uj.student.wzorceprojektowe.model.Money;
import pl.edu.uj.student.wzorceprojektowe.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.model.Vendor;

@Repository
public class OfferRepository {

  private static final String SELECT_BY_ID_TEMPLATE =
      """
            SELECT o.id, o.name, o.currency, o.amount, o.description,
                   v.name AS vendor_name
            FROM offer o JOIN vendor v ON o.vendor_id = v.id
            WHERE o.id = ?
            """;
  private static final String SEARCH_TEMPLATE =
      """
            SELECT o.id, o.name, o.currency, o.amount, o.description,
                   v.name AS vendor_name
            FROM offer o JOIN vendor v ON o.vendor_id = v.id
            WHERE o.name LIKE ? AND v.name LIKE ?
            """;
  public static final String INSERT_TEMPLATE =
      "INSERT INTO offer(name, vendor_id, currency, amount, description) VALUES (?, (SELECT id FROM vendor WHERE name = ?), ?, ?, ?)";
  public static final String COUNT_QUERY = "SELECT COUNT(*) FROM offer";

  private final JdbcTemplate jdbc;

  public OfferRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<Offer> findById(long id) {
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
    jdbc.update(
        INSERT_TEMPLATE,
        offer.name(),
        offer.vendor().name(),
        offer.price().getCurrency(),
        offer.price().getAmount(),
        offer.description());
    return offer;
  }

  public long count() {
    Long count = jdbc.queryForObject(COUNT_QUERY, Long.class);
    return count == null ? 0 : count;
  }

  private Offer mapOffer(ResultSet rs, int rowNum) throws SQLException {
    return new Offer(
        rs.getString("name"),
        new Vendor(rs.getString("vendor_name")),
        new Money(rs.getString("currency"), rs.getDouble("amount")),
        rs.getString("description"));
  }
}
