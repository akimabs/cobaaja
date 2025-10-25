# Composite Adapter Pattern Guide

> 📖 **Prerequisites:** Read [PURE_HEXAGONAL_ARCHITECTURE.md](PURE_HEXAGONAL_ARCHITECTURE.md) first to understand ports & adapters.

## 🎯 **Problem**
Butuh akses **Redis** (cache) dulu, baru **Minio** (storage) kalau cache miss.

---

## ❌ **WRONG Approach**

### Don't Put Logic in Controller!
```java
@RestController
public class PostController {
    private final PostRedisAdapter redis;
    private final PostMinioAdapter minio;
    
    @GetMapping("/posts/{id}")
    public Mono<Post> getPost(@PathVariable Long id) {
        return redis.loadById(id)              // ❌ Controller knows Redis
            .switchIfEmpty(minio.loadById(id)); // ❌ Controller knows Minio
    }
}
```

**Why wrong?**
- Controller **tahu** detail infrastructure (Redis, Minio)
- Hard to test
- Violates Single Responsibility
- Not flexible

---

## ✅ **CORRECT Approach: Composite Adapter**

### Architecture Flow:
```
┌─────────────────────┐
│   PostController    │ ← Knows ONLY GetPostUseCase (Input Port)
└──────────┬──────────┘
           │ uses
┌──────────▼──────────┐
│    PostService      │ ← Knows ONLY LoadPostPort (Output Port)
└──────────┬──────────┘
           │ depends on
┌──────────▼──────────┐
│  LoadPostPort       │ ← Interface (Output Port)
│    (interface)      │
└──────────┬──────────┘
           │ implements
┌──────────▼─────────────────────┐
│ PostCachedStorageAdapter       │ ← Composite Adapter
│                                 │
│  ├─ PostRedisAdapter  (cache)  │ ← Uses other adapters
│  └─ PostMinioAdapter  (storage)│
└─────────────────────────────────┘
```

---

## 📝 **Implementation Steps**

### 1. Create Individual Adapters

```java
// Redis Adapter (Secondary Adapter)
@Component
@Qualifier("redis")
public class PostRedisAdapter implements LoadPostPort {
    // Redis logic only
}

// Minio Adapter (Secondary Adapter)
@Component
@Qualifier("minio")
public class PostMinioAdapter implements LoadPostPort {
    // Minio logic only
}
```

### 2. Create Composite Adapter

```java
@Component
@Primary  // ← This becomes the default
public class PostCachedStorageAdapter implements LoadPostPort {
    
    private final PostRedisAdapter redis;
    private final PostMinioAdapter minio;
    
    public PostCachedStorageAdapter(
        @Qualifier("redis") PostRedisAdapter redis,
        @Qualifier("minio") PostMinioAdapter minio
    ) {
        this.redis = redis;
        this.minio = minio;
    }
    
    @Override
    public Mono<Post> loadById(Long id) {
        // Cache-Aside Pattern
        return redis.loadById(id)
            .switchIfEmpty(
                minio.loadById(id)
                    .flatMap(post -> 
                        redis.save(post).thenReturn(post)
                    )
            );
    }
}
```

> 💡 **See:** [DI_INJECTION_GUIDE.md](DI_INJECTION_GUIDE.md) for detailed Spring injection explanation.

### 3. Controller & Service Stay Clean

```java
// Controller - unchanged!
@RestController
public class PostController {
    private final GetPostUseCase getPostUseCase;  // ✅ Only knows Input Port
}

// Service - unchanged!
@Service
public class PostService implements GetPostUseCase {
    private final LoadPostPort loadPostPort;  // ✅ Only knows Output Port
}
```

---

## 🔥 **Caching Patterns**

### 1. **Cache-Aside** (Lazy Loading)
```java
public Mono<Post> loadById(Long id) {
    return cache.loadById(id)
        .switchIfEmpty(
            storage.loadById(id)
                .flatMap(post -> cache.save(post).thenReturn(post))
        );
}
```

**Flow:**
1. Try cache
2. If miss → get from storage
3. Save to cache
4. Return data

---

### 2. **Write-Through**
```java
public Mono<Void> save(Post post) {
    return storage.save(post)
        .then(cache.save(post));  // Cache after storage success
}
```

**Flow:**
1. Save to storage first
2. Then save to cache

---

### 3. **Write-Behind** (Async)
```java
public Mono<Void> save(Post post) {
    return cache.save(post)
        .doOnSuccess(p -> 
            storage.save(p).subscribe()  // Async, don't wait
        );
}
```

**Flow:**
1. Save to cache immediately
2. Save to storage async

---

### 4. **Read-Through**
```java
@Component
@Primary
public class PostReadThroughAdapter implements LoadPostPort {
    
    public Mono<Post> loadById(Long id) {
        return cache.loadById(id)
            .switchIfEmpty(
                loadFromStorageAndCache(id)
            );
    }
    
    private Mono<Post> loadFromStorageAndCache(Long id) {
        return storage.loadById(id)
            .flatMap(post -> 
                cache.save(post).thenReturn(post)
            );
    }
}
```

---

## 🎯 **Real-World Example**

### Scenario: E-commerce Product Catalog

```java
@Component
@Primary
public class ProductCatalogAdapter implements LoadProductPort {
    
    private final RedisAdapter cache;       // Fast, short TTL
    private final PostgresAdapter database; // Persistent
    private final S3Adapter images;         // Large files
    
    @Override
    public Mono<Product> loadById(Long id) {
        return cache.loadById(id)
            .switchIfEmpty(
                database.loadById(id)
                    .flatMap(product -> 
                        // Load image URL from S3
                        images.getImageUrl(product.getImageKey())
                            .map(url -> product.withImageUrl(url))
                    )
                    .flatMap(product -> 
                        cache.save(product).thenReturn(product)
                    )
            );
    }
}
```

**Flow:**
1. Check Redis (fast)
2. If miss → Query PostgreSQL
3. Get image URL from S3
4. Cache in Redis
5. Return product

**Controller doesn't know about Redis, PostgreSQL, or S3!** ✨

---

## 🛡️ **Circuit Breaker Pattern** (Bonus)

Handle failures gracefully:

```java
@Component
@Primary
public class ResilientPostAdapter implements LoadPostPort {
    
    private final PostRedisAdapter cache;
    private final PostMinioAdapter storage;
    private final PostApiClient fallback;  // External API
    
    public Mono<Post> loadById(Long id) {
        return cache.loadById(id)
            .onErrorResume(e -> {
                // Redis down? Try storage
                return storage.loadById(id);
            })
            .switchIfEmpty(
                storage.loadById(id)
                    .onErrorResume(e -> {
                        // Storage down? Try external API
                        return fallback.loadById(id);
                    })
            );
    }
}
```

---

## ✅ **Benefits**

1. **Separation of Concerns**: Each adapter has ONE job
2. **Testable**: Mock individual adapters
3. **Flexible**: Change caching strategy without touching controller
4. **Maintainable**: Clear responsibilities

---

## 📦 **Spring Bean Configuration**

```java
@Configuration
public class AdapterConfig {
    
    @Bean
    @Qualifier("redis")
    public LoadPostPort redisAdapter() {
        return new PostRedisAdapter();
    }
    
    @Bean
    @Qualifier("minio")
    public LoadPostPort minioAdapter() {
        return new PostMinioAdapter();
    }
    
    @Bean
    @Primary
    public LoadPostPort compositeAdapter(
        @Qualifier("redis") LoadPostPort redis,
        @Qualifier("minio") LoadPostPort minio
    ) {
        return new PostCachedStorageAdapter(redis, minio);
    }
}
```

---

## 🎓 **Summary**

| Layer | Responsibility | Knows About |
|-------|---------------|-------------|
| Controller | HTTP handling | Input Port (GetPostUseCase) |
| Service | Business logic | Output Port (LoadPostPort) |
| Composite Adapter | Orchestration | Other adapters |
| Individual Adapter | One technology | Redis/Minio/DB |

**The magic:** Controller & Service never change when you add/remove adapters! 🪄

---

## 📚 Related Guides

- **[PURE_HEXAGONAL_ARCHITECTURE.md](PURE_HEXAGONAL_ARCHITECTURE.md)** - Main architecture guide
- **[DI_INJECTION_GUIDE.md](DI_INJECTION_GUIDE.md)** - How Spring wires composite adapters
- **[HEXAGONAL_MAPPING_GUIDE.md](HEXAGONAL_MAPPING_GUIDE.md)** - Where to do mapping

