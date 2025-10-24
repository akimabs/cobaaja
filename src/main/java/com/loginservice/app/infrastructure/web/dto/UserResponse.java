package com.loginservice.app.infrastructure.web.dto;

import com.loginservice.app.domain.entity.User;

/**
 * DTO - User Response
 * Infrastructure layer: API response format
 * 
 * Using Record for immutable response (no need for setters)
 */
public record UserResponse(
    Long id,
    String name,
    String username,
    String email,
    String phone,
    String website
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getUsername(),
            user.getEmail(),
            user.getPhone(),
            user.getWebsite()
        );
    }
}
