package com.loginservice.app.infrastructure.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loginservice.app.application.port.in.GetPostUseCase;
import com.loginservice.app.infrastructure.web.dto.PostResponse;

import reactor.core.publisher.Mono;

/**
 * REST Controller for Posts (Primary Adapter)
 * 
 * Pure Hexagonal Architecture:
 * - Primary Adapter (Driving adapter)
 * - Drives the application by calling Input Ports (Use Cases)
 * - Controller depends on INPUT PORT, not concrete Service
 * 
 * Responsibility:
 * 1. Handle HTTP requests
 * 2. Call Use Case (Input Port)
 * 3. Convert Domain Entity → Response DTO (JSON)
 * 4. Return HTTP response
 * 
 * ❌ TIDAK TAHU tentang Service implementation atau infrastructure detail!
 * ✅ HANYA TAHU tentang GetPostUseCase (Input Port)
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {
    
    private final GetPostUseCase getPostUseCase;

    // Inject INPUT PORT (not concrete service)
    public PostController(GetPostUseCase getPostUseCase) {
        this.getPostUseCase = getPostUseCase;
    }

    /**
     * GET /api/posts/{id}
     * 
     * Flow:
     * 1. Extract path variable: id
     * 2. Call getPostUseCase.getPost(id) → returns Mono<Post> (domain entity)
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
        return getPostUseCase.getPost(id)        // Call use case (input port)
            .map(PostResponse::from);            // Convert domain → DTO for JSON
    }

    /**
     * GET /api/posts
     * 
     * Returns all posts as JSON array
     */
    @GetMapping
    public Mono<List<PostResponse>> getAllPosts() {
        return getPostUseCase.getAllPosts()      // Call use case (input port)
            .map(posts -> posts.stream()
                .map(PostResponse::from)         // Convert each Post → PostResponse
                .toList()                        // Collect to List<PostResponse>
            );
        // Spring WebFlux auto-serialize to JSON array
    }
}
