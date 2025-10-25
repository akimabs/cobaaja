# 🏛️ Pure Hexagonal Architecture (Netflix Style)

## ✅ Successfully Refactored!

This project has been refactored from **DDD-style** to **Pure Hexagonal Architecture** following Netflix best practices.

---

## 📋 New Structure

```
src/main/java/com/loginservice/app/
├── domain/                          # 🎯 DOMAIN LAYER (Pure Business Objects)
│   └── entity/                      # Domain entities ONLY
│       ├── Post.java                # Post domain entity
│       └── User.java                # User domain entity
│
├── application/                     # 🔧 APPLICATION LAYER (Use Cases & Ports)
│   ├── port/
│   │   ├── in/                     # 🔵 INPUT PORTS (Primary Ports)
│   │   │   ├── GetPostUseCase.java      # What the app OFFERS
│   │   │   └── GetUserUseCase.java
│   │   └── out/                    # 🟢 OUTPUT PORTS (Secondary Ports)
│   │       ├── LoadPostPort.java        # What the app NEEDS
│   │       └── LoadUserPort.java
│   └── service/
│       ├── PostService.java        # Implements IN port, uses OUT port
│       └── UserService.java
│
└── infrastructure/                  # 🔌 INFRASTRUCTURE LAYER (Adapters)
    ├── client/                     # Secondary Adapters (OUT)
    │   ├── post/
    │   │   ├── PostApiClient.java       # Implements LoadPostPort
    │   │   └── PostDto.java
    │   └── user/
    │       ├── UserApiClient.java       # Implements LoadUserPort
    │       └── UserDto.java
    ├── composite/
    │   └── PostCachedDbAdapter.java     # Implements LoadPostPort (composite)
    └── web/                        # Primary Adapters (IN)
        ├── PostController.java          # Uses GetPostUseCase
        ├── UserController.java          # Uses GetUserUseCase
        └── dto/
            ├── PostResponse.java
            └── UserResponse.java
```

---

## 🔑 Key Changes from DDD to Pure Hexagonal

### **Before (DDD Style):**
```
domain/
├── entity/
│   └── Post.java
└── repository/              ❌ Repository in domain
    └── PostRepository.java

application/
└── service/
    └── PostService.java     (depends on PostRepository)

infrastructure/
└── client/
    └── PostApiClient.java   (implements PostRepository)
```

### **After (Pure Hexagonal):**
```
domain/
└── entity/
    └── Post.java            ✅ Pure domain - no interfaces!

application/
├── port/
│   ├── in/
│   │   └── GetPostUseCase.java    ✅ Input Port
│   └── out/
│       └── LoadPostPort.java       ✅ Output Port
└── service/
    └── PostService.java            ✅ Implements IN, uses OUT

infrastructure/
├── web/
│   └── PostController.java         ✅ Uses IN port
└── client/
    └── PostApiClient.java          ✅ Implements OUT port
```

---

## 🎯 Flow Diagram

### **Complete Request Flow:**

```
┌─────────────────────────────────────────────────────────────┐
│                      HTTP Request                           │
│                   GET /api/posts/1                          │
└──────────────────────────┬──────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│  PRIMARY ADAPTER (Infrastructure - IN)                      │
│  PostController                                             │
│  - Handle HTTP request                                      │
│  - Depends on: GetPostUseCase (Input Port)                 │
└──────────────────────────┬──────────────────────────────────┘
                           ↓
                   getPostUseCase.getPost(1)
                           ↓
┌─────────────────────────────────────────────────────────────┐
│  INPUT PORT (Application - Primary Port)                    │
│  GetPostUseCase (interface)                                 │
│  - Defines what app offers                                  │
│  - Method: Mono<Post> getPost(Long id)                     │
└──────────────────────────┬──────────────────────────────────┘
                           ↓
                    (implemented by)
                           ↓
┌─────────────────────────────────────────────────────────────┐
│  SERVICE (Application Layer)                                │
│  PostService                                                │
│  - Implements: GetPostUseCase                              │
│  - Uses: LoadPostPort                                       │
│  - Contains business logic                                  │
└──────────────────────────┬──────────────────────────────────┘
                           ↓
                   loadPostPort.loadById(1)
                           ↓
┌─────────────────────────────────────────────────────────────┐
│  OUTPUT PORT (Application - Secondary Port)                 │
│  LoadPostPort (interface)                                   │
│  - Defines what app needs                                   │
│  - Method: Mono<Post> loadById(Long id)                    │
└──────────────────────────┬──────────────────────────────────┘
                           ↓
                    (implemented by)
                           ↓
┌─────────────────────────────────────────────────────────────┐
│  SECONDARY ADAPTER (Infrastructure - OUT)                   │
│  PostApiClient                                              │
│  - Implements: LoadPostPort                                 │
│  - Technical detail: WebClient, API call                    │
└──────────────────────────┬──────────────────────────────────┘
                           ↓
                    External API Call
                           ↓
┌─────────────────────────────────────────────────────────────┐
│            JSONPlaceholder API                              │
│            https://jsonplaceholder.typicode.com/posts/1     │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Dependency Direction

### **The Hexagonal Rule:**
```
Dependencies always point INWARD → to the core

Infrastructure → Application → Domain
   (adapters)      (ports)     (entities)
```

### **Concrete Example:**

```java
// ❌ WRONG (DDD style)
domain/repository/PostRepository.java  // Interface in domain
    ↑
    |
application/service/PostService.java   // Service depends on domain interface
    ↑
    |
infrastructure/client/PostApiClient.java  // Adapter implements domain interface
```

```java
// ✅ CORRECT (Pure Hexagonal)
domain/entity/Post.java                // Pure domain entity
    ↑
    |
application/port/out/LoadPostPort.java  // Output port (interface in application)
    ↑
    |
application/service/PostService.java    // Service depends on output port
    ↑
    |
infrastructure/client/PostApiClient.java  // Adapter implements output port
```

---

## 🔌 Ports & Adapters

### **1️⃣ Input Ports (Primary Ports)**

**Location:** `application/port/in/`

**Purpose:** Define what the application OFFERS to the outside world

**Example:**
```java
// application/port/in/GetPostUseCase.java
public interface GetPostUseCase {
    Mono<Post> getPost(Long id);
    Mono<List<Post>> getAllPosts();
}
```

**Implemented by:** Services (application layer)
```java
// application/service/PostService.java
@Service
public class PostService implements GetPostUseCase {
    // Implementation here
}
```

**Used by:** Primary Adapters (driving adapters)
```java
// infrastructure/web/PostController.java
@RestController
public class PostController {
    private final GetPostUseCase getPostUseCase;
    // Uses the input port
}
```

---

### **2️⃣ Output Ports (Secondary Ports)**

**Location:** `application/port/out/`

**Purpose:** Define what the application NEEDS from external systems

**Example:**
```java
// application/port/out/LoadPostPort.java
public interface LoadPostPort {
    Mono<Post> loadById(Long id);
    Mono<List<Post>> loadAll();
}
```

**Implemented by:** Secondary Adapters (driven adapters)
```java
// infrastructure/client/post/PostApiClient.java
@Component
public class PostApiClient implements LoadPostPort {
    // Implementation here
}
```

**Used by:** Services (application layer)
```java
// application/service/PostService.java
@Service
public class PostService implements GetPostUseCase {
    private final LoadPostPort loadPostPort;
    // Uses the output port
}
```

---

### **3️⃣ Primary Adapters (Driving Adapters)**

**Location:** `infrastructure/web/`, `infrastructure/cli/`, etc.

**Purpose:** Drive the application (trigger use cases)

**Examples:**
- REST Controllers
- GraphQL Resolvers
- CLI Commands
- Message Queue Consumers

**Characteristics:**
- ✅ Depends on INPUT PORTS (Use Cases)
- ❌ Does NOT know about Service implementation
- ❌ Does NOT know about infrastructure details

```java
// infrastructure/web/PostController.java
@RestController
public class PostController {
    private final GetPostUseCase getPostUseCase;  // ✅ Input Port
    
    @GetMapping("/{id}")
    public Mono<PostResponse> getPost(@PathVariable Long id) {
        return getPostUseCase.getPost(id)
            .map(PostResponse::from);
    }
}
```

---

### **4️⃣ Secondary Adapters (Driven Adapters)**

**Location:** `infrastructure/client/`, `infrastructure/persistence/`, etc.

**Purpose:** Fulfill application needs (implement output ports)

**Examples:**
- API Clients
- Database Repositories
- File Systems
- Cache (Redis)
- Message Brokers

**Characteristics:**
- ✅ Implements OUTPUT PORTS
- ❌ Application doesn't know which adapter is used
- ✅ Can be swapped without changing application logic

```java
// infrastructure/client/post/PostApiClient.java
@Component
public class PostApiClient implements LoadPostPort {  // ✅ Output Port
    
    @Override
    public Mono<Post> loadById(Long id) {
        // Technical detail: WebClient API call
        return webClient.get()
            .uri("/posts/{id}", id)
            .retrieve()
            .bodyToMono(PostDto.class)
            .map(this::toDomain);
    }
}
```

---

## 🎓 Key Principles

### **1. Domain is Pure**
```java
domain/
└── entity/
    └── Post.java  // No interfaces, no annotations, pure Java
```

### **2. Ports Define Contracts**
```java
application/port/
├── in/           # What app offers
└── out/          # What app needs
```

### **3. Services Bridge Ports**
```java
@Service
public class PostService implements GetPostUseCase {  // IN port
    private final LoadPostPort loadPostPort;          // OUT port
    
    @Override
    public Mono<Post> getPost(Long id) {
        return loadPostPort.loadById(id)  // Delegate to OUT port
            .filter(Post::isValid);       // Business logic
    }
}
```

### **4. Adapters are Replaceable**
```java
// Can have multiple implementations of LoadPostPort:
@Component
public class PostApiClient implements LoadPostPort { ... }

@Component
@Primary
public class PostCachedDbAdapter implements LoadPostPort { ... }

@Component
public class PostJpaAdapter implements LoadPostPort { ... }

// Service doesn't know which one is injected!
```

---

## 🧪 Testing Benefits

### **Easy to Mock Ports:**

```java
class PostServiceTest {
    
    @Mock
    private LoadPostPort loadPostPort;  // ✅ Mock output port
    
    @InjectMocks
    private PostService postService;
    
    @Test
    void getPost_shouldReturnPost() {
        // Given
        when(loadPostPort.loadById(1L))
            .thenReturn(Mono.just(new Post(1L, 1L, "Title", "Body")));
        
        // When
        Mono<Post> result = postService.getPost(1L);
        
        // Then
        StepVerifier.create(result)
            .expectNextMatches(post -> post.title().equals("Title"))
            .verifyComplete();
    }
}
```

**No need to:**
- ❌ Setup database
- ❌ Setup Redis
- ❌ Mock WebClient
- ❌ Start containers

**Just mock the port!** ✅

---

## 🔄 How to Add New Features

### **Example: Save Post**

#### **Step 1: Create Output Port**
```java
// application/port/out/SavePostPort.java
public interface SavePostPort {
    Mono<Post> save(Post post);
}
```

#### **Step 2: Create Input Port**
```java
// application/port/in/CreatePostUseCase.java
public interface CreatePostUseCase {
    Mono<Post> createPost(CreatePostCommand command);
}
```

#### **Step 3: Implement in Service**
```java
// application/service/PostService.java
@Service
public class PostService implements GetPostUseCase, CreatePostUseCase {
    
    private final LoadPostPort loadPostPort;
    private final SavePostPort savePostPort;  // NEW
    
    @Override
    public Mono<Post> createPost(CreatePostCommand command) {
        Post post = Post.create(command);  // Business logic
        return savePostPort.save(post);    // Use output port
    }
}
```

#### **Step 4: Add Primary Adapter**
```java
// infrastructure/web/PostController.java
@PostMapping
public Mono<PostResponse> createPost(@RequestBody CreatePostRequest request) {
    return createPostUseCase.createPost(request.toCommand())
        .map(PostResponse::from);
}
```

#### **Step 5: Add Secondary Adapter**
```java
// infrastructure/client/post/PostApiClient.java
@Component
public class PostApiClient implements LoadPostPort, SavePostPort {
    
    @Override
    public Mono<Post> save(Post post) {
        return webClient.post()
            .uri("/posts")
            .bodyValue(toDto(post))
            .retrieve()
            .bodyToMono(PostDto.class)
            .map(this::toDomain);
    }
}
```

---

## 📊 Before vs After Comparison

| Aspect | **DDD Style (Before)** | **Pure Hexagonal (After)** |
|--------|------------------------|---------------------------|
| **Repository Location** | `domain/repository/` | `application/port/out/` |
| **Domain Layer** | Entity + Interface | Entity ONLY |
| **Naming** | `PostRepository` | `LoadPostPort` / `SavePostPort` |
| **Controller Dependency** | `PostService` (concrete) | `GetPostUseCase` (interface) |
| **Service Dependency** | `PostRepository` | `LoadPostPort` |
| **Adapter Implements** | `PostRepository` | `LoadPostPort` |
| **Philosophy** | Repository = Domain concept | Port = Application boundary |
| **Testability** | Good | Excellent |
| **Flexibility** | Good | Excellent |
| **Explicitness** | Medium | Very High |

---

## ✅ Checklist for Pure Hexagonal

- [x] Domain has NO interfaces (only entities)
- [x] Input Ports in `application/port/in/`
- [x] Output Ports in `application/port/out/`
- [x] Services implement Input Ports
- [x] Services use Output Ports
- [x] Controllers depend on Input Ports
- [x] Adapters implement Output Ports
- [x] No direct dependency from Controller to Service
- [x] No infrastructure imports in domain/application

---

## 🚀 Benefits Achieved

### **1. Clear Separation of Concerns**
- Domain = Business entities
- Application = Use cases & contracts
- Infrastructure = Technical details

### **2. Testability**
- Mock ports, not concrete implementations
- No infrastructure needed for unit tests

### **3. Flexibility**
- Swap adapters without changing core logic
- Add new adapters for same port

### **4. Maintainability**
- Clear boundaries between layers
- Explicit contracts (ports)

### **5. Team Collaboration**
- Frontend can work on Primary Adapters
- Backend can work on Secondary Adapters
- Domain/Application can be developed independently

---

## 📚 References

- **Alistair Cockburn** - Creator of Hexagonal Architecture
- **Netflix Tech Blog** - [Ready for changes with Hexagonal Architecture](https://netflixtechblog.com/ready-for-changes-with-hexagonal-architecture-b315ec967749)
- **Robert C. Martin (Uncle Bob)** - Clean Architecture

---

**🎉 Pure Hexagonal Architecture Successfully Implemented!**

*The application is now structured following Netflix best practices with clear ports and adapters.*

