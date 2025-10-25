package com.loginservice.app.infrastructure.client.post;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.loginservice.app.application.port.out.LoadPostPort;
import com.loginservice.app.domain.entity.Post;

import reactor.core.publisher.Mono;

/**
 * Post API Client Adapter (Secondary Adapter)
 * 
 * Pure Hexagonal Architecture:
 * - IMPLEMENTS Output Port (LoadPostPort)
 * - Loads posts from external API
 * - This is infrastructure detail that app doesn't know about
 * 
 * Technical details:
 * - Uses WebClient to call JSONPlaceholder API
 * - Converts PostDto → Post domain entity
 */
@Component
public class PostApiClient implements LoadPostPort {

    private final WebClient webClient;

    public PostApiClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Load post by ID from external API
     */
    @Override
    public Mono<Post> loadById(Long id) {
        return webClient.get()
                .uri("/posts/{id}", id)
                .retrieve()
                .bodyToMono(PostDto.class)
                .map(dto -> new Post(
                    dto.getUserId(),
                    dto.getId(), 
                    dto.getTitle(), 
                    dto.getBody()
                ));
    }

    /**
     * Load all posts from external API
     */
    @Override
    public Mono<List<Post>> loadAll() {
        return webClient.get()
                .uri("/posts")
                .retrieve()
                .bodyToFlux(PostDto.class)
                .map(dto -> new Post(
                    dto.getUserId(),
                    dto.getId(), 
                    dto.getTitle(), 
                    dto.getBody()
                ))
                .filter(Post::isValid)
                .collectList();
    }
}
