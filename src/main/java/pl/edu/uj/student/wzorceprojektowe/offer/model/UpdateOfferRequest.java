package pl.edu.uj.student.wzorceprojektowe.offer.model;

import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;

/**
 * Partial update request for an offer. All fields except {@code id} are optional; absent fields are
 * left unchanged. {@code quantity} is {@link Integer} (boxed) so that an absent value can be
 * distinguished from an explicit {@code 0}.
 */
public record UpdateOfferRequest(
    String id, String name, Vendor vendor, Money price, String description, Integer quantity) {}
