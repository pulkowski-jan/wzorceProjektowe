package pl.edu.uj.student.wzorceprojektowe.offer.repository;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Vendor;

@Repository
@RequiredArgsConstructor
public class VendorRepository {
  public static final String SELECT_BY_NAME_TEMPLATE = "SELECT name FROM vendor WHERE name = ?";
  public static final String INSERT_TEMPLATE =
      "INSERT INTO vendor(name) VALUES (?) ON CONFLICT(name) DO NOTHING";
  private final JdbcTemplate jdbc;

  public Optional<Vendor> findByName(String name) {
    return jdbc
        .query(SELECT_BY_NAME_TEMPLATE, (rs, rowNum) -> new Vendor(rs.getString("name")), name)
        .stream()
        .findFirst();
  }

  public Vendor insert(Vendor vendor) {
    int inserted = jdbc.update(INSERT_TEMPLATE, vendor.name());
    if (inserted == 0) {
      throw new DuplicateKeyException("Vendor name already exists");
    }
    return new Vendor(vendor.name());
  }
}
