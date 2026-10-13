package pl.edu.uj.student.wzorceprojektowe.controller;

import org.apache.logging.log4j.util.Strings;
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
        return ResponseEntity.ok(offerRepository.search(name, vendor));
    }

    @PostMapping
    public ResponseEntity<Offer> create(@RequestBody Offer offer) {
        if (isOfferInvalid(offer)) {
            return ResponseEntity.badRequest().build();
        }
        Vendor vendor = vendorRepository.findOrCreate(offer.vendor().name());
        return ResponseEntity.ok(offerRepository.insert(
                new Offer("", offer.name(), vendor, offer.price(), offer.description(), offer.quantity())));
    }

    private static boolean isOfferInvalid(Offer offer) {
        return offer.vendor() == null || Strings.isBlank(offer.vendor().name())
                || Strings.isBlank(offer.name()) || offer.price() == null || offer.quantity() < 0;
    }
}
