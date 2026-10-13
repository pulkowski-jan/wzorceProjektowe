package pl.edu.uj.student.wzorceprojektowe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.edu.uj.student.wzorceprojektowe.model.db.VendorDatabaseRecord;

import java.util.Optional;

public interface VendorRepository extends JpaRepository<VendorDatabaseRecord, Long> {
    Optional<VendorDatabaseRecord> findByNameIgnoreCase(String name);
}
