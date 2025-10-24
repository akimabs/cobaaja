package com.loginservice.app.domain.repository;

import com.loginservice.app.domain.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository Interface - User
 * Defines contract for data access, implemented by infrastructure layer
 * 
 * Netflix Pattern: Repository interface in domain layer
 */
public interface UserRepository {
    
    Mono<User> findById(Long id);
    
    Flux<User> findAll();
}

