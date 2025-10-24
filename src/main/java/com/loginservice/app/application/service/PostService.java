package com.loginservice.app.application.service;

import com.loginservice.app.domain.entity.Post;
import com.loginservice.app.domain.repository.PostRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.List;

/**
 * Post Service (Application Layer)
 * 
 * Responsibility:
 * 1. Business logic
 * 2. Orchestrate domain operations
 * 3. Call repository (interface only!)
 * 
 * ❌ TIDAK TAHU tentang Redis, Minio, API, atau infrastructure!
 * ✅ HANYA TAHU tentang PostRepository interface
 */
@Service
public class PostService {
    
    private final PostRepository postRepository;

    // ✅ Constructor injection (best practice)
    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * Get post by ID dengan business logic
     * 
     * Business rules:
     * - Post must be valid
     * - Return error if not found or invalid
     * 
     * Note: Service tidak tahu bahwa repository adalah composite adapter
     * yang orchestrate Redis → Minio → API. Service cuma tahu interface!
     */
    public Mono<Post> getPost(Long id) {
        return postRepository.findById(id)        // Call repository (injected adapter)
            .filter(Post::isValid)
            .switchIfEmpty(
                Mono.error(new RuntimeException("Post not found or invalid: " + id))
            );
    }

    /**
     * Get all posts
     * 
     * Returns raw list from repository
     * Controller will convert to DTO
     */
    public Mono<List<Post>> getAllPosts() {
        return postRepository.findAll();
    }
}
