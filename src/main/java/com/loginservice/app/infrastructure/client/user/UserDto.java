package com.loginservice.app.infrastructure.client.user;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO - User from external API
 * Infrastructure layer: external API contract
 * 
 * Using Lombok:
 * - @Data = auto getters + setters + equals + hashCode + toString
 * - @NoArgsConstructor = default constructor for Jackson
 */
@Data
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String phone;
    private String website;
}
