# Hexagonal Architecture - Learning Project

Project ini adalah implementasi **Pure Hexagonal Architecture** (Netflix Style) dengan Spring Boot dan WebFlux.

---

## Documentation Index

### Core Guides (Recommended reading order)

1. **[PURE_HEXAGONAL_ARCHITECTURE.md](docs/PURE_HEXAGONAL_ARCHITECTURE.md)**
   - **START HERE** - Main guide untuk hexagonal architecture
   - Penjelasan layer: Domain, Application, Infrastructure
   - Penjelasan Ports & Adapters pattern
   - Flow diagram lengkap
   - Before vs After comparison (DDD → Pure Hexagonal)
   - **Baca ini dulu sebelum yang lain!**
   - **Prerequisites:** None (start here!)

2. **[HEXAGONAL_MAPPING_GUIDE.md](docs/HEXAGONAL_MAPPING_GUIDE.md)**
   - **CRITICAL** - Di mana mapping DTO ↔ Domain dilakukan
   - Separation of concerns: Domain vs Service vs Adapter
   - Kapan logic dianggap "terlalu banyak"
   - Rule of thumb untuk clean code
   - Case study lengkap: Pulsa Balance feature
   - Real-world example: Bill Payment refactoring
   - **Wajib baca untuk understand responsibilities per layer!**
   - **Prerequisites:** PURE_HEXAGONAL_ARCHITECTURE.md

---

### Advanced Patterns

3. **[COMPOSITE_ADAPTER_GUIDE.md](docs/COMPOSITE_ADAPTER_GUIDE.md)**
   - Pattern untuk handle multiple providers/implementations
   - Contoh: Multiple payment providers (Telkomsel, XL, Indosat)
   - Routing logic di adapter layer
   - Strategy pattern implementation
   - **Baca kalau ada multiple external systems**
   - **Prerequisites:** PURE_HEXAGONAL_ARCHITECTURE.md

4. **[DI_INJECTION_GUIDE.md](docs/DI_INJECTION_GUIDE.md)**
   - Dependency Injection best practices
   - Constructor injection vs Field injection
   - How Spring autowiring works dengan ports
   - @Primary, @Qualifier usage
   - Testing dengan DI
   - **Baca untuk understand Spring wiring**
   - **Prerequisites:** COMPOSITE_ADAPTER_GUIDE.md

---

### Testing

5. **[TDD_HEXAGONAL_WORKFLOW.md](docs/TDD_HEXAGONAL_WORKFLOW.md)**
   - TDD workflow specific untuk hexagonal architecture
   - Test-first approach
   - Mocking ports vs mocking implementations
   - Unit test vs Integration test
   - Testing pyramid dalam hexagonal context
   - **Baca untuk TDD best practices**
   - **Prerequisites:** PURE_HEXAGONAL_ARCHITECTURE.md, HEXAGONAL_MAPPING_GUIDE.md

---

### Technical

6. **[REACTIVE_GUIDE.md](docs/REACTIVE_GUIDE.md)**
   - Reactive programming dengan WebFlux
   - Mono vs Flux
   - Reactive patterns
   - Error handling dalam reactive streams
   - BlockHound untuk detect blocking calls
   - **Baca kalau pakai reactive programming**
   - **Prerequisites:** None (can read anytime, used across all layers)

7. **[BLOCKHOUND_GUIDE.md](docs/BLOCKHOUND_GUIDE.md)**
   - Detect blocking calls in reactive code
   - Automatic error detection
   - Common mistakes & fixes
   - Testing with BlockHound
   - **Baca untuk ensure truly non-blocking code**
   - **Prerequisites:** REACTIVE_GUIDE.md

---

## Running the Application

### Quick Start (Default: DEV mode with BlockHound)
```bash
mvn spring-boot:run
```

### Production Mode (BlockHound disabled)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Test Endpoints
```bash
# Get User (with DTO → Domain mapping)
curl http://localhost:8080/api/users/1

# Get Post
curl http://localhost:8080/api/posts/1
```

**Check logs untuk verify:**
- DEV: `BlockHound ENABLED` - Detects blocking calls
- PROD: `BlockHound DISABLED` - Max performance

---

## Learning Path

### Getting Started:

1. Read **[PURE_HEXAGONAL_ARCHITECTURE.md](docs/PURE_HEXAGONAL_ARCHITECTURE.md)** - Core concepts
2. Read **[HEXAGONAL_MAPPING_GUIDE.md](docs/HEXAGONAL_MAPPING_GUIDE.md)** - Practical implementation
3. Explore code in `src/` - Real examples

### Advanced Topics:

- **[COMPOSITE_ADAPTER_GUIDE.md](docs/COMPOSITE_ADAPTER_GUIDE.md)** - Multiple providers pattern
- **[TDD_HEXAGONAL_WORKFLOW.md](docs/TDD_HEXAGONAL_WORKFLOW.md)** - Test-driven development
- **[BLOCKHOUND_GUIDE.md](docs/BLOCKHOUND_GUIDE.md)** - Reactive non-blocking validation

---

## Project Structure

```
app/
├── README.md                        # Main documentation index
├── docs/                            # Documentation
│   ├── PURE_HEXAGONAL_ARCHITECTURE.md
│   ├── HEXAGONAL_MAPPING_GUIDE.md
│   ├── COMPOSITE_ADAPTER_GUIDE.md
│   ├── DI_INJECTION_GUIDE.md
│   ├── TDD_HEXAGONAL_WORKFLOW.md
│   ├── REACTIVE_GUIDE.md
│   └── BLOCKHOUND_GUIDE.md
│
├── pom.xml                          # Maven dependencies
│
└── src/main/java/com/loginservice/app/
    ├── domain/                      # DOMAIN LAYER
    │   └── entity/                  # Pure business entities
    │       ├── Post.java
    │       └── User.java
    │
    ├── application/                 # APPLICATION LAYER
    │   ├── port/
    │   │   ├── in/                  # Input Ports (Use Cases)
    │   │   │   ├── GetPostUseCase.java
    │   │   │   └── UserUseCase.java
    │   │   └── out/                 # Output Ports (Dependencies)
    │   │       ├── LoadPostPort.java
    │   │       └── UserPort.java
    │   └── service/
    │       ├── PostService.java     # Implements IN port, uses OUT port
    │       └── UserService.java
    │
    ├── config/                      # CONFIGURATION
    │   ├── WebClientConfig.java
    │   ├── LogFilter.java
    │   └── BlockHoundConfig.java    # Detect blocking calls
    │
    └── infrastructure/              # INFRASTRUCTURE LAYER
        ├── client/                  # Secondary Adapters (OUT)
        │   └── post/
        │       ├── PostApiClient.java   # Implements LoadPostPort
        │       └── PostDto.java         # DTO (infrastructure concern)
        ├── composite/
        │   └── PostCachedDbAdapter.java # Composite adapter pattern
        └── web/                     # Primary Adapters (IN)
            ├── PostController.java  # Uses GetPostUseCase
            └── dto/
                └── PostResponse.java # Response DTO
```

---

## Key Principles

### 1. Dependency Direction
```
Infrastructure → Application → Domain
   (adapters)      (ports)     (entities)

Dependencies always point INWARD
```

### 2. Mapping Locations
```
External API (DTO) → ADAPTER → Domain
Domain → ADAPTER → HTTP Response (DTO)

Service NEVER touches DTO!
```

### 3. Responsibilities

| Layer | Responsibility | Example |
|-------|---------------|---------|
| **Domain** | Business rules | Validation, calculation |
| **Service** | Orchestration | Call ports, apply business logic |
| **Adapter** | Technical details + **MAPPING** | API calls, DTO ↔ Domain |

---

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=PostServiceTest
```

---

## Core Concepts

**Ports:**
- **Input Ports** (Primary): What app OFFERS → Use Cases
- **Output Ports** (Secondary): What app NEEDS → Dependencies

**Adapters:**
- **Primary** (Driving): Call the app → Controllers
- **Secondary** (Driven): Called by app → API clients, DB repos

**Flow:**
```
Controller → Input Port → Service → Output Port → API Client
```

**Golden Rules:**
1. Domain is pure (no annotations)
2. Ports define contracts (interfaces)
3. Services bridge ports (implement IN, use OUT)
4. **Adapters do mapping** (DTO ↔ Domain)
5. Dependencies point inward (Infra → App → Domain)

---

## Additional Resources

- **Alistair Cockburn** - Creator of Hexagonal Architecture
- **Netflix Tech Blog** - [Ready for changes with Hexagonal Architecture](https://netflixtechblog.com/ready-for-changes-with-hexagonal-architecture-b315ec967749)
- **Robert C. Martin** - Clean Architecture

---

## FAQ

**Q: Di mana mapping DTO ↔ Domain?**  
A: Di **Adapter**, bukan Service!

**Q: Kenapa Port, bukan Repository?**  
A: Port lebih explicit (IN vs OUT). Repository adalah DDD concept.

**Q: Service boleh call API langsung?**  
A: **TIDAK!** Harus via Output Port.

**Q: Controller inject Service langsung?**  
A: **TIDAK!** Inject Input Port (Use Case interface).

---

## Benefits

- Testability - Mock ports  
- Flexibility - Swap adapters  
- Maintainability - Clear boundaries  
- Tech independence - Core logic framework-agnostic

---

**Happy Learning!**

*Architecture = boundaries & dependencies, not tools & frameworks.*
