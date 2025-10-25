# Domain-Driven Design Implementation

Implementasi aplikasi dengan **Domain-Driven Design (DDD)** pattern menggunakan Spring Boot dan WebFlux.

---

## Struktur Arsitektur

```
Domain (Repository interface)
  ↓ implements
Infrastructure (Adapter + Implementation)
  ↑ inject
Application (Service)
```

### Karakteristik DDD di Project Ini:

- **Repository pattern** - Interface di Domain layer
- **Domain model** sebagai center of application
- **Service layer** - Orchestration dan business logic
- **Adapter layer** - External system integration (API, DB, etc)
- **Reactive programming** - WebFlux, Mono/Flux

---

## Quick Start

### Jalankan Aplikasi
```bash
mvn spring-boot:run
```

### Test Endpoints
```bash
# Get User
curl http://localhost:8080/api/users/1

# Get Post
curl http://localhost:8080/api/posts/1
```

---

## Project Structure

```
src/main/java/com/loginservice/app/
├── domain/                           # DOMAIN LAYER
│   ├── entity/
│   │   ├── Post.java                 # Domain entity (record)
│   │   └── User.java                 # Domain entity (record)
│   └── repository/
│       ├── PostRepository.java       # Repository interface
│       └── UserRepository.java       # Repository interface
│
├── application/                      # APPLICATION LAYER
│   └── service/
│       ├── PostService.java          # Business logic + orchestration
│       └── UserService.java          # Business logic + orchestration
│
├── infrastructure/                   # INFRASTRUCTURE LAYER
│   ├── client/                       # External API adapters
│   │   ├── post/
│   │   │   ├── PostApiClient.java    # Implements PostRepository
│   │   │   └── PostDto.java          # External API DTO
│   │   └── user/
│   │       ├── UserApiClient.java    # Implements UserRepository
│   │       └── UserDto.java          # External API DTO
│   ├── composite/
│   │   └── PostCachedDbAdapter.java  # Composite pattern (cache + API)
│   └── web/                          # HTTP controllers
│       ├── PostController.java
│       ├── UserController.java
│       └── dto/
│           ├── PostResponse.java
│           └── UserResponse.java
│
└── config/                           # CONFIGURATION
    ├── WebClientConfig.java
    └── LogFilter.java
```

---

## Dokumentasi

### Core Concepts
- **[REPOSITORY_VS_SERVICE_GUIDE.md](docs/REPOSITORY_VS_SERVICE_GUIDE.md)** - Kapan pakai Repository vs Service
- **[ADAPTER_GUIDE.md](docs/ADAPTER_GUIDE.md)** - Pattern adapter untuk external system
- **[SIMPLIFY_WITH_RECORDS.md](docs/SIMPLIFY_WITH_RECORDS.md)** - Java Records untuk immutable entities

### Flow Examples
- **[SIMPLE_FLOW_EXAMPLE.md](docs/SIMPLE_FLOW_EXAMPLE.md)** - Flow sederhana GET request
- **[FULL_FLOW_EXAMPLE.md](docs/FULL_FLOW_EXAMPLE.md)** - Flow lengkap dengan semua layer

### Advanced Patterns
- **[COMPOSITE_ADAPTER_GUIDE.md](docs/COMPOSITE_ADAPTER_GUIDE.md)** - Multiple implementations (cache + API)
- **[DI_INJECTION_GUIDE.md](docs/DI_INJECTION_GUIDE.md)** - Dependency Injection best practices

### Technical
- **[REACTIVE_GUIDE.md](docs/REACTIVE_GUIDE.md)** - Reactive programming dengan WebFlux
- **[TDD_GUIDE.md](docs/TDD_GUIDE.md)** - Test-driven development

---

## Key Principles

### 1. Repository di Domain Layer
```java
// domain/repository/UserRepository.java
public interface UserRepository {
    Mono<User> findById(Long id);
}

// infrastructure/client/user/UserApiClient.java
@Component
public class UserApiClient implements UserRepository {
    // Implementation dengan external API
}
```

### 2. Service Inject Repository
```java
@Service
public class UserService {
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

### 3. Adapter Pattern untuk External System
```java
// Adapter bertanggung jawab:
// - Call external API
// - Transform DTO ↔ Domain Entity
// - Handle technical details
```

---

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=UserServiceTest
```

---

## Perbedaan dengan Hexagonal Architecture

| Aspek | DDD (Branch ini) | Hexagonal |
|-------|------------------|-----------|
| Interface location | Repository di Domain | Port di Application |
| Focus | Domain model centric | Application boundary |
| Service depends on | Repository (domain) | Port (application) |
| Flexibility | Domain-driven | Adapter-driven |

**Kesamaan:**
- Sama-sama pakai Adapter pattern
- Sama-sama reactive
- Sama-sama pisahkan concern

---

## Branch Lain

Ingin coba arsitektur berbeda?

```bash
# Hexagonal Architecture (Pure Ports & Adapters)
git checkout hexagonal

# Navigation/Main
git checkout main
```

---

**Happy Coding with DDD!**

