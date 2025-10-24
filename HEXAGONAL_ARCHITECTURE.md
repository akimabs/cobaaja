# 🏛️ Hexagonal Architecture (Ports & Adapters)

## 📋 Struktur Project

```
src/main/java/com/loginservice/app/
├── domain/                          # 🎯 DOMAIN LAYER (Core Business Logic)
│   ├── Post.java                   # Domain Entity - Pure Java, no framework
│   └── exception/                  # Domain Exceptions
│       ├── PostNotFoundException.java
│       └── InvalidPostException.java
│
├── application/                     # 🔧 APPLICATION LAYER (Use Cases)
│   ├── port/
│   │   ├── in/                     # Input Ports (Primary Ports)
│   │   │   ├── GetPostUseCase.java
│   │   │   ├── GetAllPostsUseCase.java
│   │   │   └── GetPostByUserIdUseCase.java
│   │   └── out/                    # Output Ports (Secondary Ports)
│   │       └── LoadPostPort.java
│   └── service/
│       └── PostService.java        # Use Case Implementation
│
└── adapter/                         # 🔌 ADAPTERS (Infrastructure)
    ├── in/                         # Primary Adapters (Driving)
    │   └── web/
    │       ├── PostController.java # REST API Adapter
    │       └── PostResponse.java   # Response DTO
    └── out/                        # Secondary Adapters (Driven)
        └── external/
            ├── PostApiAdapter.java # External API Adapter
            └── PostExternalDto.java # External DTO
```

---

## 🎯 Konsep Hexagonal Architecture

### **The Hexagon (Core)**
```
                    🌐 HTTP Request
                          ↓
              ┌───────────────────────┐
              │  Primary Adapter      │
              │  (PostController)     │
              └───────────┬───────────┘
                          ↓
              ┌───────────────────────┐
              │   Input Port          │
              │   (GetPostUseCase)    │
              └───────────┬───────────┘
                          ↓
    ╔═══════════════════════════════════════╗
    ║         APPLICATION LAYER             ║
    ║       (PostService)                   ║
    ║  - Orchestrates business logic        ║
    ║  - Uses Domain entities               ║
    ║  - Calls Output Ports                 ║
    ╚═══════════════════════════════════════╝
                          ↓
              ┌───────────────────────┐
              │   Output Port         │
              │   (LoadPostPort)      │
              └───────────┬───────────┘
                          ↓
              ┌───────────────────────┐
              │  Secondary Adapter    │
              │  (PostApiAdapter)     │
              └───────────┬───────────┘
                          ↓
                    🌍 External API
```

---

## 🔑 Prinsip Utama

### 1️⃣ **Dependency Rule**
- Dependency selalu mengarah **KE DALAM** (ke domain)
- Domain tidak tahu apa-apa tentang framework atau infrastructure
- Adapter bergantung pada Port, bukan sebaliknya

### 2️⃣ **Ports (Interfaces)**
- **Input Ports** = Apa yang bisa dilakukan aplikasi (Use Cases)
- **Output Ports** = Apa yang dibutuhkan aplikasi (Dependencies)

### 3️⃣ **Adapters (Implementations)**
- **Primary Adapters** = Yang memicu aplikasi (REST API, CLI, Queue Consumer)
- **Secondary Adapters** = Yang dipanggil aplikasi (Database, External API, File System)

---

## ✅ Keuntungan Hexagonal Architecture

### 🧪 **Testability**
```java
// Easy to mock - hanya perlu mock LoadPostPort
PostService service = new PostService(mockLoadPostPort);
```

### 🔄 **Flexibility**
```
Ganti External API → Buat adapter baru → Implement LoadPostPort
Ganti REST ke gRPC → Buat adapter baru → Panggil GetPostUseCase
```

### 🎯 **Business Logic Isolation**
```
Domain + Application = Pure business logic
Adapters = Technical details
```

### 📦 **Framework Independence**
```
Domain tidak import Spring/Reactor
Bisa pindah framework tanpa ubah business logic
```

---

## 📝 Contoh Flow: Get Post by ID

### **Request Flow:**
```
1. HTTP GET /api/posts/1
   ↓
2. PostController.getPost(1)
   ↓
3. GetPostUseCase.getPostById(1)
   ↓
4. PostService.getPostById(1)
   ↓
5. LoadPostPort.loadPostById(1)
   ↓
6. PostApiAdapter.loadPostById(1)
   ↓
7. WebClient → External API
   ↓
8. Response: PostExternalDto
   ↓
9. Convert to Domain: Post
   ↓
10. Return through layers
   ↓
11. Convert to PostResponse
   ↓
12. HTTP Response JSON
```

---

## 🆚 Hexagonal vs Clean Architecture

| Aspek | **Hexagonal** | **Clean Architecture** |
|-------|---------------|------------------------|
| **Layers** | 2 area (Inside/Outside) | 4 layers (Entities/Use Cases/Interface/Framework) |
| **Konsep** | Ports & Adapters | Concentric circles |
| **Kompleksitas** | Lebih sederhana | Lebih kompleks |
| **Fleksibilitas** | Sangat fleksibel | Sangat rigid |
| **Use Case** | Medium projects | Enterprise projects |

---

## 🎓 Best Practices

### ✅ **DO**
- Keep domain pure (no framework imports)
- Use interfaces for ports
- Adapters depend on ports, not vice versa
- One adapter per external system
- DTOs for external communication

### ❌ **DON'T**
- Import Spring in domain
- Mix business logic in adapters
- Direct coupling between adapters
- Return domain entities from API
- Business logic in controllers

---

## 🚀 Cara Menambahkan Feature Baru

### **Example: Create Post**

#### 1. **Domain** (jika perlu entity baru)
```java
// domain/Post.java - add validation method if needed
```

#### 2. **Application - Input Port**
```java
// application/port/in/CreatePostUseCase.java
public interface CreatePostUseCase {
    Mono<Post> createPost(CreatePostCommand command);
}
```

#### 3. **Application - Output Port**
```java
// application/port/out/SavePostPort.java
public interface SavePostPort {
    Mono<Post> savePost(Post post);
}
```

#### 4. **Application - Service**
```java
// application/service/PostService.java
@Override
public Mono<Post> createPost(CreatePostCommand command) {
    Post post = Post.create(command);
    return savePostPort.savePost(post);
}
```

#### 5. **Primary Adapter**
```java
// adapter/in/web/PostController.java
@PostMapping
public Mono<PostResponse> createPost(@RequestBody CreatePostRequest request) {
    return createPostUseCase.createPost(request.toCommand())
        .map(PostResponse::from);
}
```

#### 6. **Secondary Adapter**
```java
// adapter/out/external/PostApiAdapter.java
@Override
public Mono<Post> savePost(Post post) {
    return webClient.post()
        .uri("/posts")
        .bodyValue(toDto(post))
        .retrieve()
        .bodyToMono(PostExternalDto.class)
        .map(this::toDomain);
}
```

---

## 📚 References

- **Alistair Cockburn** - Creator of Hexagonal Architecture
- **Robert C. Martin (Uncle Bob)** - Clean Architecture
- **Netflix Tech Blog** - Hexagonal Architecture in Production

---

**🎉 Selamat! Kode Anda sekarang mengikuti Hexagonal Architecture!**

