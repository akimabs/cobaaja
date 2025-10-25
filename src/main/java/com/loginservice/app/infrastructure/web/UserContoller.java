package com.loginservice.app.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loginservice.app.application.port.in.UserUseCase;
import com.loginservice.app.domain.entity.User;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserContoller {
    private final UserUseCase userUseCase;

    public UserContoller(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @GetMapping("/{id}")
    public Mono<User> getUserById(@PathVariable Long id) {
        return userUseCase.getUserById(id);
    }
}