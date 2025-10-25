package com.loginservice.app.application.port.in;

import com.loginservice.app.domain.entity.User;
import reactor.core.publisher.Mono;

public interface UserUseCase {
    Mono<User> getUserById(Long id);
}
