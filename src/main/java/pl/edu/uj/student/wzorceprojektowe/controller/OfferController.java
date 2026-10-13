package pl.edu.uj.student.wzorceprojektowe.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.uj.student.wzorceprojektowe.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.model.db.OfferDatabaseRecord;
import pl.edu.uj.student.wzorceprojektowe.model.Vendor;
import pl.edu.uj.student.wzorceprojektowe.model.db.VendorDatabaseRecord;
import pl.edu.uj.student.wzorceprojektowe.repository.OfferRepository;
import pl.edu.uj.student.wzorceprojektowe.repository.VendorRepository;

import java.util.List;

@RestController
@RequestMapping("/offers")
public class OfferController {

    private final OfferRepository offerRepository;
    private final VendorRepository vendorRepository;

    public OfferController(OfferRepository offerRepository, VendorRepository vendorRepository) {
        this.offerRepository = offerRepository;
        this.vendorRepository = vendorRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Offer> byId(@PathVariable Long id) {
        return offerRepository.findById(id)
                .map(OfferDatabaseRecord::toOffer)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Offer>> search(@RequestParam(required = false, defaultValue = "") String name,
                                              @RequestParam(required = false, defaultValue = "") String vendor) {
        if (name.isBlank() && vendor.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(offerRepository
                .findByNameContainingIgnoreCaseAndVendor_NameContainingIgnoreCase(name, vendor)
                .stream().map(OfferDatabaseRecord::toOffer).toList());
    }

    @PostMapping
    public ResponseEntity<Offer> create(@RequestBody Offer offer) {
        VendorDatabaseRecord vendorEntity = resolveVendor(offer.vendor());
        if (vendorEntity == null) {
            return ResponseEntity.badRequest().build();
        }
        OfferDatabaseRecord saved = offerRepository.save(new OfferDatabaseRecord(offer, vendorEntity));
        return ResponseEntity.ok(saved.toOffer());
    }

    private VendorDatabaseRecord resolveVendor(Vendor vendor) {
        if (vendor == null || (vendor.name() == null && vendor.id() == null)) {
            return null;
        }
        if (vendor.id() != null) {
            return vendorRepository.findById(vendor.id()).orElse(null);
        }
        return vendorRepository.findByNameIgnoreCase(vendor.name())
                .orElseGet(() -> vendorRepository.save(new VendorDatabaseRecord(vendor.name())));
    }
}
