package com.loginservice.app.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class PostTest {
    
    @Test
    void testIsValidPost() {
        // Given
        Post post = new Post(1L, 1L, "Title", "Body");
        
        // When & Then
        assertTrue(post.isValid());
    }
    
    @Test
    void testGetAllPosts() {
        // Given - simulate list of posts
        Post post1 = new Post(1L, 1L, "Title One", "Body One");
        Post post2 = new Post(1L, 2L, "Title Two", "Body Two");
        Post post3 = new Post(2L, 3L, "Title Three", "Body Three");
        
        List<Post> posts = List.of(post1, post2, post3);
        
        // When
        int size = posts.size();
        
        // Then
        assertEquals(3, size);
        assertEquals("Title One", posts.get(0).title());
        assertEquals("Title Two", posts.get(1).title());
        assertEquals("Title Three", posts.get(2).title());
    }
}
