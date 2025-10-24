package com.loginservice.app.infrastructure.web.dto;

import com.loginservice.app.domain.entity.Post;

/**
 * Response DTO for HTTP JSON response
 * 
 * Converts domain entity → JSON format
 * This is what client receives as JSON
 */
public record PostResponse(
    Long userId,
    Long id,
    String title,
    String body
) {
    /**
     * Factory method: Domain Entity → Response DTO
     * 
     * Controller calls this to convert domain to JSON response
     */
    public static PostResponse from(Post post) {
        return new PostResponse(
            post.userId(),
            post.id(),
            post.title(),
            post.body()
        );
    }
}
