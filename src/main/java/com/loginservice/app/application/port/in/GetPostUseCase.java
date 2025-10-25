package com.loginservice.app.application.port.in;

import com.loginservice.app.domain.entity.Post;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Input Port (Primary Port) - Get Post Use Case
 * 
 * Pure Hexagonal Architecture:
 * - This is what the APPLICATION OFFERS (capabilities)
 * - Service IMPLEMENTS this port
 * - Controllers/Adapters USE this port
 * 
 * Use Cases:
 * - Get post by ID with validation
 * - Get all posts
 * 
 * Note: This defines WHAT the application can do
 * Controllers depend on THIS interface, not on concrete Service
 */
public interface GetPostUseCase {
    
    /**
     * Get post by ID with business rules validation
     * 
     * Business rules:
     * - Post must exist
     * - Post must be valid
     * 
     * @param id Post ID
     * @return Mono of validated Post
     * @throws RuntimeException if post not found or invalid
     */
    Mono<Post> getPost(Long id);
    
    /**
     * Get all posts
     * 
     * @return Mono of List of Posts
     */
    Mono<List<Post>> getAllPosts();
}

