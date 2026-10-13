package pl.edu.uj.student.wzorceprojektowe.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.uj.student.wzorceprojektowe.model.Vendor;
import pl.edu.uj.student.wzorceprojektowe.model.db.VendorDatabaseRecord;
import pl.edu.uj.student.wzorceprojektowe.repository.VendorRepository;

@RestController
@RequestMapping("/vendors")
public class VendorController {

    private final VendorRepository repository;

    public VendorController(VendorRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vendor> byId(@PathVariable Long id) {
        return repository.findById(id)
                .map(VendorDatabaseRecord::toVendor)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Vendor> create(@RequestBody Vendor vendor) {
        VendorDatabaseRecord saved = repository.save(new VendorDatabaseRecord(vendor.name()));
        return ResponseEntity.ok(saved.toVendor());
    }
}
