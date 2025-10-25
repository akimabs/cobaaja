package com.loginservice.app.domain.entity;

public record Organization (
    String organizationName, // Renamed dari name
    String slogan,           // Renamed dari catchPhrase
    String businessType      // Renamed dari bs
) {}

