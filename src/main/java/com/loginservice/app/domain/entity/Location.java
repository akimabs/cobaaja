package com.loginservice.app.domain.entity;

public record Location (
    String fullAddress,      // Gabungan street, suite, city
    String postalCode,       // Renamed dari zipcode
    Coordinates coordinates  // Renamed dari geo
) {}

