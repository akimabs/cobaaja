package com.loginservice.app.domain.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test - User Entity
 * Testing domain business logic
 */
class UserTest {
    
    @Test
    void testIsValid_whenValidUser_returnsTrue() {
        // Given
        User user = new User(1L, "John", "johndoe", "john@example.com", "123", "site.com");
        
        // When
        boolean result = user.isValid();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testIsValid_whenNullId_returnsFalse() {
        // Given
        User user = new User(null, "John", "johndoe", "john@example.com", "123", "site.com");
        
        // When
        boolean result = user.isValid();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testIsValid_whenNullEmail_returnsFalse() {
        // Given
        User user = new User(1L, "John", "johndoe", null, "123", "site.com");
        
        // When
        boolean result = user.isValid();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testIsValid_whenInvalidEmail_returnsFalse() {
        // Given
        User user = new User(1L, "John", "johndoe", "notanemail", "123", "site.com");
        
        // When
        boolean result = user.isValid();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testIsValid_whenNullUsername_returnsFalse() {
        // Given
        User user = new User(1L, "John", null, "john@example.com", "123", "site.com");
        
        // When
        boolean result = user.isValid();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testIsValid_whenEmptyUsername_returnsFalse() {
        // Given
        User user = new User(1L, "John", "  ", "john@example.com", "123", "site.com");
        
        // When
        boolean result = user.isValid();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testHasWebsite_whenWebsitePresent_returnsTrue() {
        // Given
        User user = new User(1L, "John", "johndoe", "john@example.com", "123", "mysite.com");
        
        // When
        boolean result = user.hasWebsite();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testHasWebsite_whenWebsiteEmpty_returnsFalse() {
        // Given
        User user = new User(1L, "John", "johndoe", "john@example.com", "123", "  ");
        
        // When
        boolean result = user.hasWebsite();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testHasCompleteProfile_whenComplete_returnsTrue() {
        // Given
        User user = new User(1L, "John", "johndoe", "john@example.com", "123456", "site.com");
        
        // When
        boolean result = user.hasCompleteProfile();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void testHasCompleteProfile_whenMissingName_returnsFalse() {
        // Given
        User user = new User(1L, null, "johndoe", "john@example.com", "123", "site.com");
        
        // When
        boolean result = user.hasCompleteProfile();
        
        // Then
        assertFalse(result);
    }
}

