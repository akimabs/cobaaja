package com.loginservice.app.infrastructure.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loginservice.app.application.service.PostService;
import com.loginservice.app.infrastructure.web.dto.PostResponse;

import reactor.core.publisher.Mono;

/**
 * REST Controller for Posts
 * 
 * Responsibility:
 * 1. Handle HTTP requests
 * 2. Call Service (application layer)
 * 3. Convert Domain Entity → Response DTO (JSON)
 * 4. Return HTTP response
 * 
 * ❌ TIDAK TAHU tentang Redis, Minio, atau infrastructure detail!
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {
    
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * GET /api/posts/{id}
     * 
     * Flow:
     * 1. Extract path variable: id
     * 2. Call postService.getPost(id) → returns Mono<Post> (domain entity)
     * 3. Convert Post → PostResponse (DTO)
     * 4. Spring WebFlux serialize PostResponse → JSON
     * 5. Return JSON response to client
     * 
     * Example response:
     * {
     *   "userId": 1,
     *   "id": 123,
     *   "title": "sunt aut facere",
     *   "body": "quia et suscipit..."
     * }
     */
    @GetMapping("/{id}")
    public Mono<PostResponse> getPost(@PathVariable Long id) {
        return postService.getPost(id)           // Get domain entity from service
            .map(PostResponse::from);            // Convert domain → DTO for JSON
    }

    /**
     * GET /api/posts
     * 
     * Returns all posts as JSON array
     */
    @GetMapping
    public Mono<List<PostResponse>> getAllPosts() {
        return postService.getAllPosts()         // Get List<Post> from service
            .map(posts -> posts.stream()
                .map(PostResponse::from)         // Convert each Post → PostResponse
                .toList()                        // Collect to List<PostResponse>
            );
        // Spring WebFlux auto-serialize to JSON array
    }
}
