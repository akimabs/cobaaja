package com.loginservice.app.application.service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.loginservice.app.application.port.out.UserPort;
import com.loginservice.app.domain.entity.User;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class UserServiceTest {
    @Mock
    private UserPort userPort;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getUserById_whenUserIsValid_shouldReturnUser() {
    User notValidUser = new User(
        1L, 
        "John Doe", 
        "johndoe", 
        "john@mail.com",
        null,
        "123-456", 
        "website.com",
        null
    );
    
    when(userPort.loadById(1L)).thenReturn(Mono.just(notValidUser));
    
    // Then - EXPECT SUCCESS (return user)
    StepVerifier.create(userService.getUserById(1L))
        .expectNextMatches(user -> user.userId().equals(1L))  // ← Expect SUCCESS!
        .verifyComplete();  // ← Expect NO ERROR!
    }
}
