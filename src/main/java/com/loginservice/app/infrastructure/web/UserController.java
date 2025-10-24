package com.loginservice.app.infrastructure.web;

import com.loginservice.app.application.service.UserService;
import com.loginservice.app.infrastructure.web.dto.UserResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST Controller - User
 * Infrastructure layer: handles HTTP requests
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/{id}")
    public Mono<UserResponse> getUser(@PathVariable Long id) {
        return userService.getUser(id)
            .map(UserResponse::from);
    }
    
    @GetMapping
    public Flux<UserResponse> getAllUsers() {
        return userService.getAllUsers()
            .map(UserResponse::from);
    }
}

