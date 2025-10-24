package com.loginservice.app.infrastructure.client.post;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.loginservice.app.domain.entity.Post;
import com.loginservice.app.domain.repository.PostRepository;

import reactor.core.publisher.Mono;

@Component
public class PostApiClient implements PostRepository {

    private final WebClient webClient;

    public PostApiClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<Post> findById(Long id) {
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

    @Override
    public Mono<List<Post>> findAll() {
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
