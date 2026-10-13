package pl.edu.uj.student.wzorceprojektowe.model;

public record Offer(String id, String name, Vendor vendor, Money price, String description, int quantity) { }
