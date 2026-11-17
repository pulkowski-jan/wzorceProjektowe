package pl.edu.uj.student.wzorceprojektowe.payment.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pl.edu.uj.student.wzorceprojektowe.shared.model.BlikPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.CardPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.CashPaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Payment;
import pl.edu.uj.student.wzorceprojektowe.shared.model.PaymentDetails;
import pl.edu.uj.student.wzorceprojektowe.shared.model.PaymentMethod;
import tools.jackson.databind.json.JsonMapper;

@Repository
@RequiredArgsConstructor
public class PaymentRepository {
  private static final String INSERT_TEMPLATE =
      """
      INSERT INTO payment
        (id, reservation_id, amount, currency, payment_method, payment_method_details,
         payment_timestamp)
      VALUES (?, ?, ?, ?, ?, ?, ?)
      """;
  private static final String SELECT_BY_ID_TEMPLATE = "SELECT * FROM payment WHERE id = ?";
  private static final String SELECT_BY_RESERVATION_ID_TEMPLATE =
      "SELECT * FROM payment WHERE reservation_id = ? ORDER BY payment_timestamp, id";

  private final JdbcTemplate jdbc;
  private final JsonMapper mapper;

  public Payment insert(Payment payment) {
    PaymentDetails details = payment.paymentDetails();
    Object methodDetails =
        switch (details.paymentMethod()) {
          case CARD -> details.cardPayment();
          case CASH -> details.cashPayment();
          case BLIK -> details.blikPayment();
        };
    jdbc.update(
        INSERT_TEMPLATE,
        payment.id(),
        payment.reservationId(),
        payment.amount(),
        payment.currency(),
        details.paymentMethod().name(),
        mapper.writeValueAsString(methodDetails),
        payment.paymentTimestamp().toEpochMilli());
    return payment;
  }

  public Optional<Payment> findById(String id) {
    return jdbc.query(SELECT_BY_ID_TEMPLATE, this::mapPayment, id).stream().findFirst();
  }

  public List<Payment> findByReservationId(String reservationId) {
    return jdbc.query(SELECT_BY_RESERVATION_ID_TEMPLATE, this::mapPayment, reservationId);
  }

  private Payment mapPayment(ResultSet rs, int rowNum) throws SQLException {
    PaymentMethod paymentMethod = PaymentMethod.valueOf(rs.getString("payment_method"));
    String methodDetailsJson = rs.getString("payment_method_details");
    PaymentDetails details =
        switch (paymentMethod) {
          case CARD ->
              new PaymentDetails(
                  paymentMethod,
                  mapper.readValue(methodDetailsJson, CardPaymentDetails.class),
                  null,
                  null);
          case CASH ->
              new PaymentDetails(
                  paymentMethod,
                  null,
                  mapper.readValue(methodDetailsJson, CashPaymentDetails.class),
                  null);
          case BLIK ->
              new PaymentDetails(
                  paymentMethod,
                  null,
                  null,
                  mapper.readValue(methodDetailsJson, BlikPaymentDetails.class));
        };
    return new Payment(
        rs.getString("id"),
        rs.getString("reservation_id"),
        rs.getString("amount"),
        rs.getString("currency"),
        details,
        Instant.ofEpochMilli(rs.getLong("payment_timestamp")));
  }
}
