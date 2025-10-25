package com.loginservice.app.application.port.out;

import com.loginservice.app.domain.entity.Post;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Output Port (Secondary Port) - Load Post
 * 
 * Pure Hexagonal Architecture:
 * - This is what the APPLICATION NEEDS (dependency)
 * - Adapters IMPLEMENT this port
 * - Service DEPENDS on this port
 * 
 * Implementation examples:
 * - PostApiAdapter (load from external API)
 * - PostJpaAdapter (load from database)
 * - PostCachedDbAdapter (load from Redis → DB)
 * 
 * Note: Naming changed from "Repository" to "Port" to be explicit
 * about Hexagonal Architecture
 */
public interface LoadPostPort {
    
    /**
     * Load post by ID from any storage
     * 
     * @param id Post ID
     * @return Mono of Post if found
     */
    Mono<Post> loadById(Long id);
    
    /**
     * Load all posts from any storage
     * 
     * @return Mono of List of Posts
     */
    Mono<List<Post>> loadAll();    
}

