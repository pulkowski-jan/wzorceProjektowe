package pl.edu.uj.student.wzorceprojektowe.offer.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import pl.edu.uj.student.wzorceprojektowe.offer.exception.OfferNotFoundException;
import pl.edu.uj.student.wzorceprojektowe.offer.model.Offer;
import pl.edu.uj.student.wzorceprojektowe.offer.model.UpdateOfferRequest;
import pl.edu.uj.student.wzorceprojektowe.offer.service.OfferChangeService;
import pl.edu.uj.student.wzorceprojektowe.offer.service.OfferCreateService;
import pl.edu.uj.student.wzorceprojektowe.offer.service.OfferSearchService;
import pl.edu.uj.student.wzorceprojektowe.shared.exception.RequestValidationException;

@RestController
@RequestMapping("/offer")
@RequiredArgsConstructor
public class OfferController {

  private final OfferSearchService offerSearchService;
  private final OfferCreateService offerCreateService;
  private final OfferChangeService offerChangeService;

  @GetMapping("/{id}")
  public Offer byId(@PathVariable String id) {
    return offerSearchService.searchById(id);
  }

  @GetMapping("/search")
  public List<Offer> search(
      @RequestParam(required = false, defaultValue = "") String name,
      @RequestParam(required = false, defaultValue = "") String vendor) {
    return offerSearchService.searchByNameAndVendor(name, vendor);
  }

  @PostMapping("/create")
  public Offer create(@RequestBody Offer offer) {
    return offerCreateService.create(offer);
  }

  @PostMapping("/update")
  public Offer update(@RequestBody UpdateOfferRequest request) {
    return offerChangeService.updateOffer(request);
  }

  @DeleteMapping("/{id}")
  public Offer delete(@PathVariable String id) {
    return offerChangeService.deleteOffer(id);
  }

  @ExceptionHandler(RequestValidationException.class)
  public ErrorResponse handleRequestValidationException(RequestValidationException e) {
    return badRequest(e);
  }

  @ExceptionHandler(OfferNotFoundException.class)
  public ErrorResponse handleOfferNotFoundException(OfferNotFoundException e) {
    return notFound(e);
  }

  private static ErrorResponse badRequest(Exception e) {
    return ErrorResponse.create(e, HttpStatus.BAD_REQUEST, e.getMessage());
  }

  private static ErrorResponse notFound(Exception e) {
    return ErrorResponse.create(e, HttpStatus.NOT_FOUND, e.getMessage());
  }
}
