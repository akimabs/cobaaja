package com.loginservice.app.application.port.out;

import com.loginservice.app.domain.entity.User;
import reactor.core.publisher.Mono;

public interface UserPort {
    Mono<User> loadById(Long id);
}
