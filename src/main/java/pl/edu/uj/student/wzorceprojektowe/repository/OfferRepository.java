package pl.edu.uj.student.wzorceprojektowe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.edu.uj.student.wzorceprojektowe.model.db.OfferDatabaseRecord;

import java.util.List;

public interface OfferRepository extends JpaRepository<OfferDatabaseRecord, Long> {

    List<OfferDatabaseRecord> findByNameContainingIgnoreCaseAndVendor_NameContainingIgnoreCase(
            String name, String vendorName);
}
