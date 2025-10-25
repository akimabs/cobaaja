package com.loginservice.app.application.service;

import com.loginservice.app.application.port.in.GetPostUseCase;
import com.loginservice.app.application.port.out.LoadPostPort;
import com.loginservice.app.domain.entity.Post;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.List;

/**
 * Post Service (Application Layer)
 * 
 * Pure Hexagonal Architecture:
 * - IMPLEMENTS Input Port (GetPostUseCase) - what app offers
 * - USES Output Port (LoadPostPort) - what app needs
 * 
 * Responsibility:
 * 1. Business logic & validation
 * 2. Orchestrate domain operations
 * 3. Bridge between Input Port and Output Port
 * 
 * ❌ TIDAK TAHU tentang Redis, Minio, API, atau infrastructure!
 * ✅ HANYA TAHU tentang Ports (interfaces)
 */
@Service
public class PostService implements GetPostUseCase {
    
    private final LoadPostPort loadPostPort;

    // ✅ Constructor injection (best practice)
    // Inject OUTPUT PORT (not concrete adapter)
    public PostService(LoadPostPort loadPostPort) {
        this.loadPostPort = loadPostPort;
    }

    /**
     * Get post by ID dengan business logic
     * 
     * Business rules:
     * - Post must be valid
     * - Return error if not found or invalid
     * 
     * Note: Service tidak tahu adapter mana yang dipanggil
     * Service hanya tahu LoadPostPort interface!
     */
    @Override
    public Mono<Post> getPost(Long id) {
        return loadPostPort.loadById(id)        // Call output port (injected adapter)
            .filter(Post::isValid)
            .switchIfEmpty(
                Mono.error(new RuntimeException("Post not found or invalid: " + id))
            );
    }

    /**
     * Get all posts
     * 
     * Returns list from output port
     * Controller will convert to DTO
     */
    @Override
    public Mono<List<Post>> getAllPosts() {
        return loadPostPort.loadAll();
    }
}
