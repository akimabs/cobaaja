package com.loginservice.app.domain.repository;

import com.loginservice.app.domain.entity.Post;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Post Repository Interface (Port)
 * 
 * This is a PORT in Hexagonal Architecture
 * - Service depends on THIS interface (not concrete implementation)
 * - Adapters implement this interface
 * 
 * Spring DI will inject the adapter with @Primary annotation
 * (e.g., PostCacheStorageAdapter or PostApiClient)
 */
public interface PostRepository {
    
    /**
     * Find post by ID
     * 
     * Implementation could be:
     * - PostApiClient (get from API)
     * - PostJpaAdapter (get from database)
     * - PostCacheStorageAdapter (Redis → Minio → API)
     * 
     * Service doesn't care which one!
     */
    Mono<Post> findById(Long id);
    
    /**
     * Find all posts
     */
    Mono<List<Post>> findAll();    
}