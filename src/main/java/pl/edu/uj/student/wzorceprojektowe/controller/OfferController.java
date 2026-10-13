package pl.edu.uj.student.wzorceprojektowe.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.edu.uj.student.wzorceprojektowe.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.model.Vendor;
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
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Offer>> search(@RequestParam(required = false, defaultValue = "") String name,
                                              @RequestParam(required = false, defaultValue = "") String vendor) {
        if (name.isBlank() && vendor.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(offerRepository.search(name, vendor));
    }

    @PostMapping
    public ResponseEntity<Offer> create(@RequestBody Offer offer) {
        if (offer.vendor() == null || offer.vendor().name() == null || offer.vendor().name().isBlank()
                || offer.name() == null || offer.name().isBlank() || offer.price() == null) {
            return ResponseEntity.badRequest().build();
        }
        Vendor vendor = vendorRepository.findOrCreate(offer.vendor().name());
        return ResponseEntity.ok(offerRepository.insert(
                new Offer(offer.name(), vendor, offer.price(), offer.description())));
    }
}
