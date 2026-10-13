package pl.edu.uj.student.wzorceprojektowe.model.db;

import jakarta.persistence.Table;
import pl.edu.uj.student.wzorceprojektowe.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.model.Money;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "offer_entity")
public class OfferDatabaseRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private VendorDatabaseRecord vendor;
    private String currency;
    private double amount;
    private String description;

    protected OfferDatabaseRecord() { }

    public OfferDatabaseRecord(Offer offer, VendorDatabaseRecord vendor) {
        this.name = offer.name();
        this.vendor = vendor;
        this.currency = offer.price().getCurrency();
        this.amount = offer.price().getAmount();
        this.description = offer.description();
    }

    public Offer toOffer() {
        return new Offer(name, vendor.toVendor(), new Money(currency, amount), description);
    }

    public Long getId() { return id; }
}
