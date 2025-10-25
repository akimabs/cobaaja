package com.loginservice.app.domain.entity;

/**
 * Domain Entity - Business Model
 * Struktur sesuai kebutuhan SISTEM KITA, bukan external API
 */
public record User (
    Long userId,             // ← Renamed dari id
    String fullName,         // ← Renamed dari name
    String username,
    String email,
    Location location,       // ← Renamed dari address
    String phoneNumber,      // ← Renamed dari phone
    String websiteUrl,       // ← Renamed dari website
    Organization organization // ← Renamed dari company
) {
    public boolean isValid() {
        return userId != null &&
            fullName != null &&
            username != null &&
            email != null &&
            phoneNumber != null &&
            websiteUrl != null;
        // Note: location and organization can be null (optional fields)
    }
    
    /**
     * Business method: Get display name
     */
    public String getDisplayName() {
        return fullName + " (@" + username + ")";
    }
}