package pl.edu.uj.student.wzorceprojektowe.model.db;

import jakarta.persistence.Table;
import pl.edu.uj.student.wzorceprojektowe.model.Vendor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@Table(name = "vendor_entity")
public class VendorDatabaseRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    protected VendorDatabaseRecord() { }

    public VendorDatabaseRecord(String name) {
        this.name = name;
    }

    public Vendor toVendor() {
        return new Vendor(id, name);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
