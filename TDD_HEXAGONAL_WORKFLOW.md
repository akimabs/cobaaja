# 🔄 TDD + Hexagonal Architecture Workflow

## Untuk Feature: "Get All Posts"

### ✅ PHASE 1: DOMAIN (Pure Logic - FULL TDD)

#### Step 1: Test Domain Entity
```bash
# File: PostTest.java
```
```java
@Test
void testIsValid_whenValidPost() {
    Post post = new Post(1L, 1L, "Title", "Body");
    assertTrue(post.isValid());
}

@Test
void testFilterValidPosts_fromList() {
    Post valid = new Post(1L, 1L, "Valid", "Body");
    Post invalid = new Post(1L, 2L, "ab", "Body");
    
    List<Post> posts = List.of(valid, invalid);
    List<Post> validPosts = posts.stream()
        .filter(Post::isValid)
        .collect(Collectors.toList());
    
    assertEquals(1, validPosts.size());
}
```

#### Step 2: Run Test (RED 🔴)
```bash
mvn test -Dtest=PostTest
# Error: Post class not found
```

#### Step 3: Implement Domain Entity (GREEN 🟢)
```java
// File: domain/entity/Post.java
public record Post(Long userId, Long id, String title, String body) {
    public boolean isValid() {
        return id != null && title != null && title.length() >= 3;
    }
}
```

#### Step 4: Run Test Again (GREEN ✅)
```bash
mvn test -Dtest=PostTest
# SUCCESS!
```

#### Step 5: Define Repository Interface (Contract)
```java
// File: domain/repository/PostRepository.java
public interface PostRepository {
    Flux<Post> findAll();
    Mono<Post> findById(Long id);
}
```

**✅ DOMAIN DONE! No external dependencies, pure business logic.**

---

### ✅ PHASE 2: APPLICATION (Business Rules)

#### Step 6: Create Interactor (Use Case)
```java
// File: application/interactor/GetAllPostsInteractor.java
@Component
public class GetAllPostsInteractor {
    private final PostRepository repository;
    
    public GetAllPostsInteractor(PostRepository repository) {
        this.repository = repository;
    }
    
    public Flux<Post> execute() {
        return repository.findAll()
            .filter(Post::isValid);  // ← Business rule: only valid posts
    }
}
```

**Note:** For macOS Mockito issue, skip unit test here, test integration later.

**✅ APPLICATION DONE! Business orchestration ready.**

---

### ✅ PHASE 3: INFRASTRUCTURE (External Adapters)

#### Step 7: Create External DTO
```java
// File: infrastructure/client/post/PostDto.java
public class PostDto {
    private Long userId;
    private Long id;
    private String title;
    private String body;
    
    // Getters/Setters for Jackson deserialization
}
```

#### Step 8: Implement Repository (Adapter)
```java
// File: infrastructure/client/post/PostApiClient.java
@Component
public class PostApiClient implements PostRepository {
    
    private final WebClient webClient;
    
    public PostApiClient(WebClient webClient) {
        this.webClient = webClient;
    }
    
    @Override
    public Flux<Post> findAll() {
        return webClient.get()
            .uri("/posts")
            .retrieve()
            .bodyToFlux(PostDto.class)
            .map(this::toDomain);
    }
    
    private Post toDomain(PostDto dto) {
        return new Post(
            dto.getUserId(),
            dto.getId(),
            dto.getTitle(),
            dto.getBody()
        );
    }
}
```

#### Step 9: Create Response DTO
```java
// File: infrastructure/web/dto/PostResponse.java
public record PostResponse(Long id, String title, String body) {
    public static PostResponse from(Post post) {
        return new PostResponse(post.id(), post.title(), post.body());
    }
}
```

#### Step 10: Create Controller (HTTP Adapter)
```java
// File: infrastructure/web/PostController.java
@RestController
@RequestMapping("/api/posts")
public class PostController {
    
    private final GetAllPostsInteractor getAllPostsInteractor;
    
    public PostController(GetAllPostsInteractor getAllPostsInteractor) {
        this.getAllPostsInteractor = getAllPostsInteractor;
    }
    
    @GetMapping
    public Flux<PostResponse> getAllPosts() {
        return getAllPostsInteractor.execute()
            .map(PostResponse::from);
    }
}
```

**✅ INFRASTRUCTURE DONE! External connections ready.**

---

### ✅ PHASE 4: INTEGRATION TEST

#### Step 11: Run Application
```bash
mvn spring-boot:run
```

#### Step 12: Test Endpoint
```bash
curl http://localhost:8080/api/posts
```

Expected:
```json
[
  {
    "id": 1,
    "title": "Post Title",
    "body": "Post Body"
  },
  ...
]
```

---

## 🎯 SUMMARY - TDD Flow for Each Layer

### Domain Layer (FULL TDD)
1. ✅ Write test for entity logic
2. ✅ Run test (RED 🔴)
3. ✅ Implement entity
4. ✅ Run test (GREEN 🟢)
5. ✅ Refactor (if needed)
6. ✅ Define interfaces (contracts)

### Application Layer (Business Logic)
1. ⚠️ Write interactor (skip test for now due to Mockito issue)
2. ✅ Inject repository interface
3. ✅ Implement business rules

### Infrastructure Layer (Adapters)
1. ✅ Create DTOs (external contracts)
2. ✅ Implement repository adapter (API client)
3. ✅ Create response DTOs
4. ✅ Create controller (HTTP adapter)
5. ✅ Test integration (manual or Postman)

---

## 📊 Dependency Direction

```
Infrastructure ──depends on──> Application ──depends on──> Domain
   (Adapters)                  (Use Cases)               (Core Logic)

   Controller  ────────────────> Interactor ──────────> Post Entity
   ApiClient   ────────────────> Repository Interface
```

**Key Rule:** Dependencies point INWARD!
- ✅ Infrastructure depends on Application
- ✅ Application depends on Domain
- ❌ Domain NEVER depends on anything!

---

## 🔥 Why This Order?

### 1. Domain First (Inside-Out)
- ✅ Pure business logic
- ✅ No framework dependency
- ✅ Easy to test (no mocking)
- ✅ Defines what you NEED

### 2. Application Second (Orchestration)
- ✅ Uses domain entities
- ✅ Defines contracts (interfaces)
- ✅ Business rules

### 3. Infrastructure Last (External)
- ✅ Implements contracts
- ✅ Connects to external world
- ✅ Can be replaced easily

---

## 💡 Pro Tips

### ✅ DO:
1. Start with domain entity tests
2. Keep domain pure (no annotations)
3. Use interfaces for repositories
4. Test domain logic thoroughly
5. Infrastructure is just "plumbing"

### ❌ DON'T:
1. Don't start with controller
2. Don't put business logic in controller
3. Don't test infrastructure first
4. Don't skip domain tests
5. Don't couple domain to framework

---

## 🎯 Current Project Status

### ✅ Done:
- Domain: Post entity with validation
- Domain: Repository interface
- Application: GetAllPostsInteractor
- Infrastructure: Ready to implement

### 🚀 Next Steps:
1. Create PostDto (external)
2. Implement PostApiClient
3. Create PostResponse
4. Create PostController
5. Test with curl!

---

## 🔄 Repeat for Each Feature!

```
Feature: Get Post by ID
├── Domain Test → Post.java (already done)
├── Application → GetPostInteractor
└── Infrastructure → Same adapter, add endpoint

Feature: Get Posts by User ID
├── Domain Test → belongsToUser() method
├── Application → GetPostsByUserIdInteractor
└── Infrastructure → Filter logic
```

**Every feature follows the same flow!** 🚀

