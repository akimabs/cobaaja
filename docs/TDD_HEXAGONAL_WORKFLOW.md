# 🔄 TDD + Hexagonal Architecture Workflow

> 📖 **Prerequisites:** Read [PURE_HEXAGONAL_ARCHITECTURE.md](PURE_HEXAGONAL_ARCHITECTURE.md) first to understand ports & adapters pattern.

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

#### Step 5: Define Output Port (Contract)
```java
// File: application/port/out/LoadPostPort.java
public interface LoadPostPort {
    Mono<List<Post>> loadAll();
    Mono<Post> loadById(Long id);
}
```

> 💡 **Note:** In Pure Hexagonal Architecture, we use **Ports** not Repositories.  
> Port = Application boundary (in `application/` layer)  
> Repository = Domain concept (DDD style - NOT used here)

**✅ DOMAIN DONE! No external dependencies, pure business logic.**

---

### ✅ PHASE 2: APPLICATION (Business Rules)

#### Step 6: Create Input Port (Use Case Interface)
```java
// File: application/port/in/GetPostUseCase.java
public interface GetPostUseCase {
    Mono<List<Post>> getAllPosts();
}
```

#### Step 7: Create Service (Implements Use Case)
```java
// File: application/service/PostService.java
@Service
public class PostService implements GetPostUseCase {
    
    private final LoadPostPort loadPostPort;
    
    public PostService(LoadPostPort loadPostPort) {
        this.loadPostPort = loadPostPort;
    }
    
    @Override
    public Mono<List<Post>> getAllPosts() {
        return loadPostPort.loadAll()
            .map(posts -> posts.stream()
                .filter(Post::isValid)  // ← Business rule: only valid posts
                .toList()
            );
    }
}
```

> 💡 **Port-based architecture:**
> - Service implements **Input Port** (GetPostUseCase)
> - Service uses **Output Port** (LoadPostPort)
> - Controller depends on Input Port, NOT concrete Service

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

#### Step 8: Implement Output Port (Secondary Adapter)
```java
// File: infrastructure/client/post/PostApiClient.java
@Component
public class PostApiClient implements LoadPostPort {
    
    private final WebClient webClient;
    
    public PostApiClient(WebClient webClient) {
        this.webClient = webClient;
    }
    
    @Override
    public Mono<List<Post>> loadAll() {
        return webClient.get()
            .uri("/posts")
            .retrieve()
            .bodyToFlux(PostDto.class)
            .map(this::toDomain)     // ✅ MAPPING in adapter
            .collectList();
    }
    
    @Override
    public Mono<Post> loadById(Long id) {
        return webClient.get()
            .uri("/posts/{id}", id)
            .retrieve()
            .bodyToMono(PostDto.class)
            .map(this::toDomain);    // ✅ MAPPING in adapter
    }
    
    /**
     * ✅ MAPPING: PostDto (external) → Post (domain)
     */
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

> 💡 **Adapter Responsibility:**
> - Implements Output Port (LoadPostPort)
> - Calls external API
> - **Does mapping: DTO → Domain** (See [HEXAGONAL_MAPPING_GUIDE.md](HEXAGONAL_MAPPING_GUIDE.md))
> - Returns domain objects to service

#### Step 9: Create Response DTO
```java
// File: infrastructure/web/dto/PostResponse.java
public record PostResponse(Long id, String title, String body) {
    public static PostResponse from(Post post) {
        return new PostResponse(post.id(), post.title(), post.body());
    }
}
```

#### Step 10: Create Controller (Primary Adapter)
```java
// File: infrastructure/web/PostController.java
@RestController
@RequestMapping("/api/posts")
public class PostController {
    
    private final GetPostUseCase getPostUseCase;
    
    // ✅ Inject Input Port, NOT concrete service
    public PostController(GetPostUseCase getPostUseCase) {
        this.getPostUseCase = getPostUseCase;
    }
    
    @GetMapping
    public Mono<List<PostResponse>> getAllPosts() {
        return getPostUseCase.getAllPosts()
            .map(posts -> posts.stream()
                .map(PostResponse::from)  // ✅ Domain → Response DTO
                .toList()
            );
    }
}
```

> 💡 **Controller Responsibility:**
> - Primary Adapter (HTTP entry point)
> - Depends on **Input Port** (GetPostUseCase), NOT Service
> - Maps domain → response DTO

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

### Application Layer (Ports & Services)
1. ✅ Define Output Port (LoadPostPort)
2. ✅ Define Input Port (GetPostUseCase)
3. ✅ Implement Service (implements IN, uses OUT)
4. ⚠️ Skip unit test for now (Mockito issue)
5. ✅ Business rules in service

### Infrastructure Layer (Adapters)
1. ✅ Create DTOs (external contracts)
2. ✅ Implement Secondary Adapter (PostApiClient implements LoadPostPort)
3. ✅ **Do mapping DTO ↔ Domain in adapter**
4. ✅ Create response DTOs
5. ✅ Create Primary Adapter (PostController uses GetPostUseCase)
6. ✅ **Do mapping Domain → Response in controller**
7. ✅ Test integration (manual or Postman)

> 📖 **Read more:** [HEXAGONAL_MAPPING_GUIDE.md](HEXAGONAL_MAPPING_GUIDE.md) for detailed mapping strategy

---

## 📊 Dependency Direction (Pure Hexagonal)

```
Infrastructure ──depends on──> Application ──depends on──> Domain
   (Adapters)                    (Ports)                 (Entities)

   Controller  ────uses────> GetPostUseCase (IN port)
                                     │
                                     ↓ implemented by
                             PostService ────uses────> LoadPostPort (OUT port)
                                                               │
                                                               ↓ implemented by
                             PostApiClient (Secondary Adapter)
```

**Key Rule:** Dependencies point INWARD!
- ✅ Infrastructure depends on Application (Ports)
- ✅ Application depends on Domain (Entities)
- ❌ Domain NEVER depends on anything!

**Port-based Flow:**
1. Controller → Input Port (interface)
2. Service implements Input Port
3. Service uses Output Port (interface)
4. Adapter implements Output Port

---

## 🔥 Why This Order?

### 1. Domain First (Inside-Out)
- ✅ Pure business logic
- ✅ No framework dependency
- ✅ No interfaces in domain (Pure Hexagonal)
- ✅ Easy to test (no mocking)

### 2. Application Second (Ports & Orchestration)
- ✅ Define Output Ports (what app NEEDS)
- ✅ Define Input Ports (what app OFFERS)
- ✅ Implement Services (bridge ports)
- ✅ Business rules & orchestration

### 3. Infrastructure Last (Adapters)
- ✅ Implement Output Ports (Secondary Adapters)
- ✅ Use Input Ports (Primary Adapters)
- ✅ **Do all mapping here**
- ✅ Connects to external world
- ✅ Can be replaced easily

---

## 💡 Pro Tips

### ✅ DO:
1. Start with domain entity tests
2. Keep domain pure (no annotations, no interfaces)
3. Define ports in application layer
4. Do mapping in adapters (NOT service)
5. Test domain logic thoroughly
6. Infrastructure is just "plumbing"

### ❌ DON'T:
1. Don't put interfaces in domain (use Ports in application)
2. Don't start with controller
3. Don't put business logic in controller
4. Don't do mapping in service
5. Don't test infrastructure first
6. Don't skip domain tests
7. Don't couple domain to framework

---

## 🎯 Current Project Status

### ✅ Done:
- Domain: Post entity with validation (pure, no interfaces)
- Application: LoadPostPort (Output Port)
- Application: GetPostUseCase (Input Port)
- Application: PostService (implements IN, uses OUT)
- Infrastructure: Ready to implement

### 🚀 Next Steps:
1. Create PostDto (external format - in infrastructure)
2. Implement PostApiClient (implements LoadPostPort)
3. Map DTO → Domain in PostApiClient
4. Create PostResponse (response format)
5. Create PostController (uses GetPostUseCase)
6. Map Domain → Response in PostController
7. Test with curl!

> 📖 **Related Guides:**
> - [PURE_HEXAGONAL_ARCHITECTURE.md](PURE_HEXAGONAL_ARCHITECTURE.md) - Architecture overview
> - [HEXAGONAL_MAPPING_GUIDE.md](HEXAGONAL_MAPPING_GUIDE.md) - Where to do mapping
> - [DI_INJECTION_GUIDE.md](DI_INJECTION_GUIDE.md) - How Spring wires everything

---

## 🔄 Repeat for Each Feature!

```
Feature: Get Post by ID
├── Domain → Post.java (already done)
├── Application/Ports → GetPostUseCase (Input), LoadPostPort (Output)
├── Application/Service → PostService.getPost(id)
└── Infrastructure → PostApiClient.loadById(), PostController.getPost()

Feature: Create Post
├── Domain → Post validation rules
├── Application/Ports → CreatePostUseCase (Input), SavePostPort (Output)
├── Application/Service → PostService.createPost(command)
└── Infrastructure → PostApiClient.save(), PostController.createPost()
```

**Every feature follows the same flow!** 🚀

---

## 📚 Related Documentation

- **[PURE_HEXAGONAL_ARCHITECTURE.md](PURE_HEXAGONAL_ARCHITECTURE.md)** - Main architecture guide
- **[HEXAGONAL_MAPPING_GUIDE.md](HEXAGONAL_MAPPING_GUIDE.md)** - Mapping strategy
- **[COMPOSITE_ADAPTER_GUIDE.md](COMPOSITE_ADAPTER_GUIDE.md)** - Multiple providers pattern
- **[DI_INJECTION_GUIDE.md](DI_INJECTION_GUIDE.md)** - Dependency injection

