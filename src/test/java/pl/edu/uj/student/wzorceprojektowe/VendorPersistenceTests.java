package pl.edu.uj.student.wzorceprojektowe;

import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pl.edu.uj.student.wzorceprojektowe.controller.VendorController;
import pl.edu.uj.student.wzorceprojektowe.model.Money;
import pl.edu.uj.student.wzorceprojektowe.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.model.Vendor;
import pl.edu.uj.student.wzorceprojektowe.repository.OfferRepository;
import pl.edu.uj.student.wzorceprojektowe.repository.VendorRepository;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VendorPersistenceTests {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) throws IOException {
        var file = Files.createTempFile("vendors-test-", ".db").toFile();
        file.deleteOnExit();
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + file.getAbsolutePath());
    }

    @Autowired VendorRepository vendors;
    @Autowired OfferRepository offers;
    @Autowired VendorController controller;

    @Test
    void duplicateVendorReturnsConflict() {
        assertEquals(200, controller.create(new Vendor("Unique Vendor")).getStatusCode().value());
        assertEquals(409, controller.create(new Vendor("Unique Vendor")).getStatusCode().value());
    }

    @Test
    void resolvingVendorIsIdempotentAndOffersUseName() {
        var vendor = vendors.findOrCreate("Reusable Vendor");
        assertEquals(vendor, vendors.findOrCreate("Reusable Vendor"));
        offers.insert(new Offer("Test Concert", vendor, new Money("PLN", 12), "test"));
        var found = offers.search("Test Concert", "Reusable Vendor");
        assertEquals(1, found.size());
        assertEquals(vendor, found.getFirst().vendor());
    }

    @Test
    void vendorJsonDoesNotExposeId() {
        assertEquals("{\"name\":\"Public Vendor\"}",
                JsonMapper.builder().build().writeValueAsString(new Vendor("Public Vendor")));
    }

    @Test
    void vendorLookupUsesName() {
        vendors.findOrCreate("Lookup Vendor");
        assertEquals(new Vendor("Lookup Vendor"), controller.byName("Lookup Vendor").getBody());
        assertEquals(404, controller.byName("Missing Vendor").getStatusCode().value());
        assertEquals(400, controller.create(new Vendor(" ")).getStatusCode().value());
    }
}
