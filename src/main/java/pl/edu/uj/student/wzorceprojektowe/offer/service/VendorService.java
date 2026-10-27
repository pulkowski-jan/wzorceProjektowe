package pl.edu.uj.student.wzorceprojektowe.offer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.uj.student.wzorceprojektowe.offer.exception.VendorNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Vendor;
import pl.edu.uj.student.wzorceprojektowe.offer.repository.VendorRepository;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;

@Service
@RequiredArgsConstructor
public class VendorService {
  private final VendorRepository vendorRepository;

  public Vendor findByName(String name) {
    return vendorRepository
        .findByName(name)
        .orElseThrow(
            () ->
                new VendorNotFoundException("Vendor with name %s does not exist".formatted(name)));
  }

  public Vendor insert(Vendor vendor) {
    if (vendor.name() == null || vendor.name().isBlank()) {
      throw new RequestValidationException("Vendor name is null or empty");
    }
    return vendorRepository.insert(vendor);
  }

  private Vendor insert(String vendorName) {
    if (vendorName == null || vendorName.isBlank()) {
      throw new RequestValidationException("Vendor name is null or empty");
    }
    return vendorRepository.insert(new Vendor(vendorName));
  }

  public Vendor findOrCreate(String name) {
    return vendorRepository.findByName(name).orElseGet(() -> insert(name));
  }
}
