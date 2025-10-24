package com.loginservice.app.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.loginservice.app.domain.entity.Post;
import com.loginservice.app.domain.repository.PostRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should return all posts when repository has data")
    void getAllPosts_whenRepositoryHasData_shouldReturnAllPosts() {
        // Given
        List<Post> mockPosts = Arrays.asList(
            new Post(1L, 1L, "Title 1", "Body 1"),
            new Post(1L, 2L, "Title 2", "Body 2"),
            new Post(2L, 3L, "Title 3", "Body 3")
        );
        
        when(postRepository.findAll()).thenReturn(Mono.just(mockPosts));

        // When
        Mono<List<Post>> result = postService.getAllPosts();

        // Then
        StepVerifier.create(result)
            .assertNext(posts -> {
                assertNotNull(posts);
                assertEquals(3, posts.size());
                assertEquals("Title 1", posts.get(0).title());
                assertEquals("Title 2", posts.get(1).title());
                assertEquals("Title 3", posts.get(2).title());
            })
            .verifyComplete();

        // Verify repository was called once
        verify(postRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when repository has no data")
    void getAllPosts_whenRepositoryIsEmpty_shouldReturnEmptyList() {
        // Given
        when(postRepository.findAll()).thenReturn(Mono.just(Collections.emptyList()));

        // When
        Mono<List<Post>> result = postService.getAllPosts();

        // Then
        StepVerifier.create(result)
            .assertNext(posts -> {
                assertNotNull(posts);
                assertTrue(posts.isEmpty());
            })
            .verifyComplete();

        verify(postRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should propagate error when repository fails")
    void getAllPosts_whenRepositoryFails_shouldPropagateError() {
        // Given
        RuntimeException expectedException = new RuntimeException("Database connection failed");
        when(postRepository.findAll()).thenReturn(Mono.error(expectedException));

        // When
        Mono<List<Post>> result = postService.getAllPosts();

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable -> 
                throwable instanceof RuntimeException &&
                throwable.getMessage().equals("Database connection failed")
            )
            .verify();

        verify(postRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return posts with valid data structure")
    void getAllPosts_shouldReturnPostsWithValidStructure() {
        // Given
        List<Post> mockPosts = Arrays.asList(
            new Post(1L, 1L, "Title", "Body")
        );
        
        when(postRepository.findAll()).thenReturn(Mono.just(mockPosts));

        // When
        Mono<List<Post>> result = postService.getAllPosts();

        // Then
        StepVerifier.create(result)
            .assertNext(posts -> {
                Post post = posts.get(0);
                assertNotNull(post.userId());
                assertNotNull(post.id());
                assertNotNull(post.title());
                assertNotNull(post.body());
                assertTrue(post.isValid());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle multiple posts from different users")
    void getAllPosts_withMultipleUsers_shouldReturnAllPosts() {
        // Given
        List<Post> mockPosts = Arrays.asList(
            new Post(1L, 1L, "User 1 Post", "Body 1"),
            new Post(2L, 2L, "User 2 Post", "Body 2"),
            new Post(1L, 3L, "User 1 Another Post", "Body 3")
        );
        
        when(postRepository.findAll()).thenReturn(Mono.just(mockPosts));

        // When
        Mono<List<Post>> result = postService.getAllPosts();

        // Then
        StepVerifier.create(result)
            .assertNext(posts -> {
                assertEquals(3, posts.size());
                
                // Verify we have posts from different users
                long user1Count = posts.stream().filter(p -> p.userId().equals(1L)).count();
                long user2Count = posts.stream().filter(p -> p.userId().equals(2L)).count();
                
                assertEquals(2, user1Count);
                assertEquals(1, user2Count);
            })
            .verifyComplete();
    }
}

