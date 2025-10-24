package com.loginservice.app.domain.entity;

/**
 * Domain Entity - User
 * Pure business object, no framework dependencies
 */
public class User {
    
    private final Long id;
    private final String name;
    private final String username;
    private final String email;
    private final String phone;
    private final String website;
    
    public User(Long id, String name, String username, String email, String phone, String website) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.website = website;
    }
    
    // Business logic methods
    public boolean isValid() {
        return id != null && 
               email != null && 
               email.contains("@") && 
               username != null && 
               !username.trim().isEmpty();
    }
    
    public boolean hasWebsite() {
        return website != null && !website.trim().isEmpty();
    }
    
    public boolean hasCompleteProfile() {
        return isValid() && name != null && phone != null;
    }
    
    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getWebsite() { return website; }
}

