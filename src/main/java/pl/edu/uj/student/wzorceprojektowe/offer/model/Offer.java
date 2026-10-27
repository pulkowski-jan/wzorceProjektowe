package pl.edu.uj.student.wzorceprojektowe.offer.model;

import pl.edu.uj.student.wzorceprojektowe.shared.model.Money;

public record Offer(
        String id, String name, Vendor vendor, Money price, String description, int quantity) {}
