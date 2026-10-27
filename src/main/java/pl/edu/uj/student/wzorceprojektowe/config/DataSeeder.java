package pl.edu.uj.student.wzorceprojektowe.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pl.edu.uj.student.wzorceprojektowe.offer.service.VendorService;
import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Vendor;
import pl.edu.uj.student.wzorceprojektowe.offer.repository.OfferRepository;

@Component
public class DataSeeder implements CommandLineRunner {

  private final OfferRepository offerRepository;
  private final VendorService vendorService;

  public DataSeeder(OfferRepository offerRepository, VendorService vendorService) {
    this.offerRepository = offerRepository;
    this.vendorService = vendorService;
  }

  @Override
  public void run(String... args) {
    if (offerRepository.count() == 0) {
      Vendor ticketmaster = vendorService.findOrCreate("Ticketmaster");
      Vendor teatrStary = vendorService.findOrCreate("Teatr Stary");
      offerRepository.insert(
          new Offer(
              "",
              "Koncert: Metallica",
              ticketmaster,
              new Money("PLN", "350.0"),
              "Bilet na koncert",
              1));
      offerRepository.insert(
          new Offer(
              "",
              "Spektakl: Hamlet",
              teatrStary,
              new Money("PLN", "120.0"),
              "Bilet na spektakl",
              1));
    }
  }
}
