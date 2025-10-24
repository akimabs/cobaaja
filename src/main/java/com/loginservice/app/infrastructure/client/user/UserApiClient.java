package com.loginservice.app.infrastructure.client.user;

import com.loginservice.app.domain.entity.User;
import com.loginservice.app.domain.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * User API Client - implements UserRepository
 * Infrastructure layer: external API integration
 * Netflix Pattern: Repository implementation in infrastructure
 */
@Component
public class UserApiClient implements UserRepository {
    
    private final WebClient webClient;
    
    public UserApiClient(WebClient webClient) {
        this.webClient = webClient;
    }
    
    @Override
    public Mono<User> findById(Long id) {
        return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .bodyToMono(UserDto.class)
            .map(this::toDomain);
    }
    
    @Override
    public Flux<User> findAll() {
        return webClient.get()
            .uri("/users")
            .retrieve()
            .bodyToFlux(UserDto.class)
            .map(this::toDomain);
    }
    
    private User toDomain(UserDto dto) {
        return new User(
            dto.getId(),
            dto.getName(),
            dto.getUsername(),
            dto.getEmail(),
            dto.getPhone(),
            dto.getWebsite()
        );
    }
}

