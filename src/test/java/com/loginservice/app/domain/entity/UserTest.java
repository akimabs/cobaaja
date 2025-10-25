package com.loginservice.app.domain.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class UserTest {
    @Test
    void testIsValidUser() {
        User user = new User(1L, "John Doe", "john.doe", "john.doe@example.com", new Location("123 Main St", "Apt 4B", new Coordinates("-37.3159", "81.1496")), "1234567890", "https://example.com", new Organization("Example Inc", "Multi-layered client-server neural-net", "harness real-time e-markets"));
        
        assertTrue(user.isValid());
    }
}
