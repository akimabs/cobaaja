# Full Flow: Request → Response JSON

## 🎯 **Scenario:** 
`GET /api/posts/123` → Return JSON response

---

## 📊 **Complete Flow Diagram:**

```
┌─────────────────────────────────────────────────────────────┐
│ 1. HTTP Request                                             │
│    GET /api/posts/123                                       │
└─────────────────┬───────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. PostController (infrastructure/web)                      │
│    - Receive HTTP request                                   │
│    - Extract path variable: id = 123                        │
│    - Call service                                           │
└─────────────────┬───────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. PostService (application/service)                        │
│    - Business logic (validation, filtering)                 │
│    - Call repository.findById(123)                          │
└─────────────────┬───────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. PostRepository (domain/repository) - INTERFACE           │
│    - Just an interface, no implementation                   │
└─────────────────┬───────────────────────────────────────────┘
                  ↓ (Spring DI)
┌─────────────────────────────────────────────────────────────┐
│ 5. PostCacheStorageAdapter (infrastructure/composite)       │
│    @Primary - Spring auto-inject this                       │
│                                                              │
│    Step 1: Check Redis                                      │
│    ├─ HIT? ✅ Return immediately                            │
│    └─ MISS? ❌ Continue...                                   │
│                                                              │
│    Step 2: Get from API (PostApiClient)                     │
│    Step 3: Save to Minio (PostMinioAdapter)                 │
│    Step 4: Cache to Redis (PostRedisAdapter)                │
│    Step 5: Return Post domain entity                        │
└─────────────────┬───────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. Back to PostService                                      │
│    - Receive Post domain entity                             │
│    - Apply business logic (filter, validate)                │
│    - Return Mono<Post>                                      │
└─────────────────┬───────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────────────┐
│ 7. Back to PostController                                   │
│    - Receive Mono<Post>                                     │
│    - Convert: Post → PostResponse (DTO)                     │
│    - Return Mono<PostResponse>                              │
└─────────────────┬───────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────────────┐
│ 8. Spring WebFlux                                           │
│    - Serialize PostResponse → JSON                          │
│    - Set Content-Type: application/json                     │
│    - Send HTTP Response                                     │
└─────────────────┬───────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────────────┐
│ 9. HTTP Response                                            │
│    {                                                         │
│      "userId": 1,                                            │
│      "id": 123,                                              │
│      "title": "sunt aut facere",                             │
│      "body": "quia et suscipit..."                           │
│    }                                                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 💻 **Code Implementation:**

### 1️⃣ **Controller (Web Layer)**

```java
package com.loginservice.app.infrastructure.web;

import com.loginservice.app.application.service.PostService;
import com.loginservice.app.infrastructure.web.dto.PostResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    
    private final PostService postService;
    
    public PostController(PostService postService) {
        this.postService = postService;
    }
    
    /**
     * GET /api/posts/{id}
     * 
     * Flow:
     * 1. Extract path variable
     * 2. Call service
     * 3. Convert domain entity → DTO
     * 4. Return JSON
     */
    @GetMapping("/{id}")
    public Mono<PostResponse> getPost(@PathVariable Long id) {
        return postService.getPost(id)           // Call service
            .map(PostResponse::from);            // Domain → DTO
    }
    
    @GetMapping
    public Mono<List<PostResponse>> getAllPosts() {
        return postService.getAllPosts()
            .map(posts -> posts.stream()
                .map(PostResponse::from)
                .toList()
            );
    }
}
```

---

### 2️⃣ **Service (Application Layer)**

```java
package com.loginservice.app.application.service;

import com.loginservice.app.domain.entity.Post;
import com.loginservice.app.domain.repository.PostRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.List;

@Service
public class PostService {
    
    private final PostRepository repository;  // Interface only!
    
    public PostService(PostRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Get post dengan business logic
     * 
     * Business rules:
     * - Post must be valid
     * - Return error if not found
     */
    public Mono<Post> getPost(Long id) {
        return repository.findById(id)        // Call repository (interface)
            .filter(Post::isValid)            // Business rule: must be valid
            .switchIfEmpty(
                Mono.error(new RuntimeException("Post not found or invalid: " + id))
            );
    }
    
    public Mono<List<Post>> getAllPosts() {
        return repository.findAll();
    }
}
```

---

### 3️⃣ **Repository Interface (Domain Layer)**

```java
package com.loginservice.app.domain.repository;

import com.loginservice.app.domain.entity.Post;
import reactor.core.publisher.Mono;
import java.util.List;

/**
 * Repository interface (Port)
 * 
 * Service depends on THIS (interface)
 * NOT on concrete implementation!
 */
public interface PostRepository {
    Mono<Post> findById(Long id);
    Mono<List<Post>> findAll();
}
```

---

### 4️⃣ **Composite Adapter (Infrastructure Layer)**

```java
package com.loginservice.app.infrastructure.composite;

import com.loginservice.app.domain.entity.Post;
import com.loginservice.app.domain.repository.PostRepository;
import com.loginservice.app.infrastructure.cache.post.PostRedisAdapter;
import com.loginservice.app.infrastructure.storage.post.PostMinioAdapter;
import com.loginservice.app.infrastructure.client.post.PostApiClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Primary  // ← Spring will inject THIS as default PostRepository
public class PostCacheStorageAdapter implements PostRepository {
    
    private final PostRedisAdapter redis;
    private final PostMinioAdapter minio;
    private final PostApiClient api;
    
    public PostCacheStorageAdapter(
        PostRedisAdapter redis,
        PostMinioAdapter minio,
        PostApiClient api
    ) {
        this.redis = redis;
        this.minio = minio;
        this.api = api;
    }
    
    @Override
    public Mono<Post> findById(Long id) {
        // Step 1: Check Redis cache
        return redis.findById(id)
            .doOnNext(post -> 
                System.out.println("✅ Cache HIT from Redis")
            )
            .switchIfEmpty(
                Mono.defer(() -> {
                    System.out.println("❌ Cache MISS");
                    
                    // Step 2: Get from API
                    return api.findById(id)
                        .flatMap(post -> 
                            // Step 3: Save to Minio
                            minio.save(post)
                                .doOnSuccess(v -> 
                                    System.out.println("💾 Saved to Minio")
                                )
                                // Step 4: Cache to Redis
                                .then(redis.save(post))
                                .doOnSuccess(v -> 
                                    System.out.println("⚡ Cached to Redis")
                                )
                                // Step 5: Return Post
                                .thenReturn(post)
                        );
                })
            );
    }
    
    @Override
    public Mono<List<Post>> findAll() {
        return api.findAll();  // For simplicity
    }
}
```

---

### 5️⃣ **Response DTO (Web Layer)**

```java
package com.loginservice.app.infrastructure.web.dto;

import com.loginservice.app.domain.entity.Post;

/**
 * Response DTO for HTTP JSON response
 * 
 * Converts domain entity → JSON format
 */
public record PostResponse(
    Long userId,
    Long id,
    String title,
    String body
) {
    /**
     * Factory method: Domain Entity → DTO
     */
    public static PostResponse from(Post post) {
        return new PostResponse(
            post.userId(),
            post.id(),
            post.title(),
            post.body()
        );
    }
}
```

---

## 🚀 **Execution Example:**

### **Request:**
```bash
curl http://localhost:8080/api/posts/123
```

### **Console Output:**
```
Step 1: Checking Redis cache...
❌ Cache MISS

Step 2: Fetching from API...
✅ Fetched from JSONPlaceholder

Step 3: Saving to Minio...
💾 Saved to Minio: posts/123.json

Step 4: Caching to Redis...
⚡ Cached to Redis with TTL: 10 min

Step 5: Returning to client...
```

### **Response (JSON):**
```json
{
  "userId": 1,
  "id": 123,
  "title": "sunt aut facere repellat provident",
  "body": "quia et suscipit\nsuscipit recusandae..."
}
```

---

## 🔍 **Layer Responsibilities:**

| Layer | File | Responsibility | Returns |
|-------|------|---------------|---------|
| **Web** | `PostController.java` | HTTP handling | `PostResponse` (JSON) |
| **Application** | `PostService.java` | Business logic | `Post` (domain) |
| **Domain** | `PostRepository.java` | Interface (port) | - |
| **Infrastructure** | `PostCacheStorageAdapter.java` | Orchestration | `Post` (domain) |
| **Infrastructure** | `PostRedisAdapter.java` | Cache ops | `Post` |
| **Infrastructure** | `PostMinioAdapter.java` | Storage ops | `void` |
| **Infrastructure** | `PostApiClient.java` | API calls | `Post` |

---

## 🎯 **Data Flow:**

### Request Direction (↓)
```
HTTP JSON Request
    ↓
PostResponse (DTO) ← parsed by Spring
    ↓
PostService ← receives parameters
    ↓
PostRepository (interface)
    ↓
PostCacheStorageAdapter
    ↓
Redis / Minio / API
```

### Response Direction (↑)
```
Redis / Minio / API
    ↓
Post (domain entity)
    ↓
PostCacheStorageAdapter
    ↓
PostRepository (interface)
    ↓
PostService
    ↓
Post (domain entity)
    ↓
PostController → PostResponse.from(post)
    ↓
PostResponse (DTO)
    ↓
Spring serialize to JSON
    ↓
HTTP JSON Response
```

---

## 🔑 **Key Points:**

1. ✅ **Controller** only knows Service (not Redis/Minio)
2. ✅ **Service** only knows Repository interface (not adapters)
3. ✅ **Domain entity** flows through layers
4. ✅ **DTO** only at web boundary (controller)
5. ✅ **Spring DI** wires everything automatically

---

## 🧪 **Testing Each Layer:**

### Controller Test
```java
@WebFluxTest(PostController.class)
class PostControllerTest {
    
    @MockBean
    private PostService postService;
    
    @Test
    void getPost_shouldReturnJson() {
        Post post = new Post(1L, 123L, "Title", "Body");
        when(postService.getPost(123L)).thenReturn(Mono.just(post));
        
        webTestClient.get()
            .uri("/api/posts/123")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(123)
            .jsonPath("$.title").isEqualTo("Title");
    }
}
```

### Service Test
```java
class PostServiceTest {
    
    @Mock
    private PostRepository repository;
    
    @InjectMocks
    private PostService service;
    
    @Test
    void getPost_shouldReturnValidPost() {
        Post post = new Post(1L, 123L, "Title", "Body");
        when(repository.findById(123L)).thenReturn(Mono.just(post));
        
        StepVerifier.create(service.getPost(123L))
            .expectNext(post)
            .verifyComplete();
    }
}
```

---

## ✅ **Summary:**

```
HTTP Request
    ↓ (JSON)
Controller → extract data, call service
    ↓
Service → business logic, call repository
    ↓ (interface)
Composite Adapter → orchestrate Redis/Minio/API
    ↓ (domain entity)
Service → receive domain entity
    ↓ (domain entity)
Controller → convert to DTO
    ↓ (JSON)
HTTP Response
```

**Magic:** Service & Controller ga tahu tentang Redis/Minio! 🪄

