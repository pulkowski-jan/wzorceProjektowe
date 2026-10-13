package pl.edu.uj.student.wzorceprojektowe.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pl.edu.uj.student.wzorceprojektowe.model.Money;
import pl.edu.uj.student.wzorceprojektowe.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.model.Vendor;
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
            Vendor ticketmaster = vendorRepository.findOrCreate("Ticketmaster");
            Vendor teatrStary = vendorRepository.findOrCreate("Teatr Stary");
            offerRepository.insert(new Offer("", "Koncert: Metallica", ticketmaster, new Money("PLN", 350.0), "Bilet na koncert", 1));
            offerRepository.insert(new Offer("", "Spektakl: Hamlet", teatrStary, new Money("PLN", 120.0), "Bilet na spektakl", 1));
        }
    }
}
