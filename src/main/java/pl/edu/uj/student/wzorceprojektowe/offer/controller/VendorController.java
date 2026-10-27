package pl.edu.uj.student.wzorceprojektowe.offer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import pl.edu.uj.student.wzorceprojektowe.offer.exception.VendorNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Vendor;
import pl.edu.uj.student.wzorceprojektowe.offer.service.VendorService;

@RestController
@RequestMapping("/vendors")
@RequiredArgsConstructor
public class VendorController {
  private final VendorService vendorService;

  @GetMapping
  public Vendor byName(@RequestParam String name) {
    return vendorService.findByName(name);
  }

  @PostMapping
  public Vendor create(@RequestBody Vendor vendor) {
    return vendorService.insert(vendor);
  }

  @ExceptionHandler(VendorNotFoundException.class)
  public ErrorResponse handleVendorNotFoundException(VendorNotFoundException e) {
    return ErrorResponse.create(e, HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(DuplicateKeyException.class)
  public ErrorResponse handleDuplicateKeyException(DuplicateKeyException e) {
    return ErrorResponse.create(e, HttpStatus.CONFLICT, e.getMessage());
  }
}
