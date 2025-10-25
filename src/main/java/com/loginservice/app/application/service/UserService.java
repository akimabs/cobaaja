package com.loginservice.app.application.service;

import org.springframework.stereotype.Service;

import com.loginservice.app.application.port.in.UserUseCase;
import com.loginservice.app.application.port.out.UserPort;
import com.loginservice.app.domain.entity.User;

import reactor.core.publisher.Mono;

@Service
public class UserService implements UserUseCase {

    private final UserPort userPort;

    public UserService(UserPort userPort) {
        this.userPort = userPort;
    }

    @Override
    public Mono<User> getUserById(Long id) {
        return userPort.loadById(id)
            .filter(User::isValid)
            .switchIfEmpty(Mono.error(new RuntimeException("User not found or invalid: " + id)));
    }
}
