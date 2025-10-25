# Spring Boot Architecture Examples

This repository contains different architectural implementations of the same application using Spring Boot and WebFlux.

---

## Available Branches

### 1. **Branch: `ddd`**
Domain-Driven Design implementation

**Switch to this branch:**
```bash
git checkout ddd
```

**What's inside:**
- Domain-Driven Design architecture
- Aggregate pattern
- Repository pattern
- Value Objects
- Domain Events

---

### 2. **Branch: `hexagonal`** (Recommended)
Pure Hexagonal Architecture (Ports & Adapters)

**Switch to this branch:**
```bash
git checkout hexagonal
```

**What's inside:**
- Hexagonal Architecture (Netflix style)
- Ports & Adapters pattern
- Clean separation of concerns
- DTO ↔ Domain mapping in adapters
- Reactive programming with WebFlux
- BlockHound for non-blocking validation
- Comprehensive documentation

**Documentation:**
- PURE_HEXAGONAL_ARCHITECTURE.md - Core concepts
- HEXAGONAL_MAPPING_GUIDE.md - Mapping patterns
- COMPOSITE_ADAPTER_GUIDE.md - Multiple providers
- DI_INJECTION_GUIDE.md - Dependency injection
- TDD_HEXAGONAL_WORKFLOW.md - Test-driven development
- REACTIVE_GUIDE.md - Reactive programming
- BLOCKHOUND_GUIDE.md - Non-blocking validation

---

## Quick Comparison

| Aspect | DDD | Hexagonal |
|--------|-----|-----------|
| Focus | Domain model | Port boundaries |
| Structure | Aggregates | Ports & Adapters |
| Dependencies | Domain → Infrastructure | Infrastructure → Application → Domain |
| Mapping | Service layer | Adapter layer |
| Best for | Complex business logic | Flexible, testable systems |

---

## Getting Started

1. Choose your preferred architecture:
   - `git checkout ddd` - For DDD approach
   - `git checkout hexagonal` - For Hexagonal approach

2. Read the README in that branch for detailed instructions

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

---

## Project Goal

Learn and compare different architectural patterns by implementing the same application:
- User management
- Post management
- External API integration
- Reactive programming
- Clean code principles

---

**Choose a branch above to get started!**

