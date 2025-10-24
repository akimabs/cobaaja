package com.loginservice.app.application.service;

import com.loginservice.app.domain.entity.User;
import com.loginservice.app.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * User Service - All user-related business logic
 * Combines all User use cases in one place
 */
@Service
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Get user by ID
     */
    public Mono<User> getUser(Long id) {
        return userRepository.findById(id)
            .filter(User::isValid)
            .switchIfEmpty(Mono.error(new RuntimeException("User not found or invalid: " + id)));
    }
    
    /**
     * Get all users
     */
    public Flux<User> getAllUsers() {
        return userRepository.findAll()
            .filter(User::isValid);
    }
}

