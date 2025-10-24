package com.loginservice.app.domain.entity;

public record Post(
    Long userId,
    Long id,
    String title,
    String body
) {
    public boolean isValid() {
        return userId != null &&
            id != null &&
            title != null &&
            body != null;
    }
}
