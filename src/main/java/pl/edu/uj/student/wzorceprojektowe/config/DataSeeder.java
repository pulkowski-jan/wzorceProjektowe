package pl.edu.uj.student.wzorceprojektowe.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pl.edu.uj.student.wzorceprojektowe.model.Money;
import pl.edu.uj.student.wzorceprojektowe.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.model.db.OfferDatabaseRecord;
import pl.edu.uj.student.wzorceprojektowe.model.Vendor;
import pl.edu.uj.student.wzorceprojektowe.model.db.VendorDatabaseRecord;
import pl.edu.uj.student.wzorceprojektowe.repository.OfferRepository;
import pl.edu.uj.student.wzorceprojektowe.repository.VendorRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final OfferRepository offerRepository;
    private final VendorRepository vendorRepository;

    public DataSeeder(OfferRepository offerRepository, VendorRepository vendorRepository) {
        this.offerRepository = offerRepository;
        this.vendorRepository = vendorRepository;
    }

    @Override
    public void run(String... args) {
        if (offerRepository.count() == 0) {
            VendorDatabaseRecord ticketmaster = vendorRepository.save(new VendorDatabaseRecord("Ticketmaster"));
            VendorDatabaseRecord teatrStary = vendorRepository.save(new VendorDatabaseRecord("Teatr Stary"));
            offerRepository.save(new OfferDatabaseRecord(
                    new Offer("Koncert: Metallica", ticketmaster.toVendor(), new Money("PLN", 350.0), "Bilet na koncert"), ticketmaster));
            offerRepository.save(new OfferDatabaseRecord(
                    new Offer("Spektakl: Hamlet", teatrStary.toVendor(), new Money("PLN", 120.0), "Bilet na spektakl"), teatrStary));
        }
    }
}
