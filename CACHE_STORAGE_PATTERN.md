# Cache + Storage Pattern Guide

## 🎯 **Use Case:**

> "Get dari Redis. Kalo ada data baru, POST ke temporary asset (Minio)"

---

## 🔄 **Flow Diagram:**

```
GET /api/posts/1
    │
    ├─ Step 1: Check Redis Cache
    │     │
    │     ├─ HIT? ✅
    │     │   └─ Return data (FAST!)
    │     │
    │     └─ MISS? ❌
    │           │
    │           ├─ Step 2: Get from Source (API/Database)
    │           │     └─ Fetch Post #1
    │           │
    │           ├─ Step 3: POST to Minio (Temporary Asset)
    │           │     └─ Save as backup/archive
    │           │
    │           ├─ Step 4: Cache to Redis (TTL 10 min)
    │           │     └─ Future requests = fast
    │           │
    │           └─ Step 5: Return data
```

---

## 💻 **Implementation:**

### 1. **Composite Adapter**

```java
@Component
@Primary
public class PostCacheStorageAdapter implements PostRepository {
    
    private final PostRedisAdapter redis;      // Cache layer
    private final PostMinioAdapter minio;      // Storage layer
    private final PostApiClient api;           // Source (API)
    
    public Mono<Post> findById(Long id) {
        // Step 1: Check cache
        return redis.findById(id)
            .doOnNext(post -> log.info("Cache HIT: {}", id))
            
            // Step 2-5: Cache miss handling
            .switchIfEmpty(
                Mono.defer(() -> {
                    log.info("Cache MISS: {}", id);
                    
                    return api.findById(id)              // Get from API
                        .flatMap(post -> 
                            minio.save(post)             // POST to Minio
                                .then(redis.save(post))  // Cache to Redis
                                .thenReturn(post)        // Return
                        );
                })
            );
    }
}
```

---

### 2. **Service Layer (Unchanged!)**

```java
@Service
public class PostService {
    
    private final PostRepository repository;  // Don't care about cache/storage!
    
    public Mono<Post> getPost(Long id) {
        return repository.findById(id)
            .filter(Post::isValid);  // Business logic only
    }
}
```

---

### 3. **Individual Adapters**

#### Redis Adapter (Cache)
```java
@Component
@Qualifier("redis")
public class PostRedisAdapter {
    
    private final RedisTemplate<String, Post> redis;
    
    public Mono<Post> findById(Long id) {
        String key = "post:" + id;
        return Mono.fromCallable(() -> 
            redis.opsForValue().get(key)
        );
    }
    
    public Mono<Void> save(Post post) {
        String key = "post:" + post.id();
        return Mono.fromRunnable(() ->
            redis.opsForValue()
                .set(key, post, Duration.ofMinutes(10))  // TTL 10 min
        );
    }
    
    public Mono<Void> delete(Long id) {
        String key = "post:" + id;
        return Mono.fromRunnable(() -> redis.delete(key));
    }
}
```

#### Minio Adapter (Storage)
```java
@Component
@Qualifier("minio")
public class PostMinioAdapter {
    
    private final MinioClient minioClient;
    
    public Mono<Void> save(Post post) {
        return Mono.fromRunnable(() -> {
            String json = toJson(post);
            String objectName = "posts/" + post.id() + ".json";
            
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket("temporary-assets")
                    .object(objectName)
                    .stream(
                        new ByteArrayInputStream(json.getBytes()),
                        json.length(),
                        -1
                    )
                    .contentType("application/json")
                    .build()
            );
        });
    }
    
    public Mono<Post> findById(Long id) {
        return Mono.fromCallable(() -> {
            String objectName = "posts/" + id + ".json";
            
            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket("temporary-assets")
                    .object(objectName)
                    .build()
            );
            
            return fromJson(stream);
        });
    }
}
```

---

## 🚀 **Execution Example:**

### First Request (Cache MISS):
```
GET /api/posts/123

→ redis.findById(123)
  ❌ MISS (not in cache)
  
→ api.findById(123)
  ✅ Fetched from API
  
→ minio.save(post)
  💾 Saved to Minio: posts/123.json
  
→ redis.save(post)
  ⚡ Cached with TTL: 10 min
  
→ Return post (took ~200ms)
```

### Second Request (Cache HIT):
```
GET /api/posts/123

→ redis.findById(123)
  ✅ HIT! (found in cache)
  
→ Return post (took ~5ms) 🚀
```

---

## ⚙️ **Configuration:**

### Redis Config
```yaml
# application.yml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 10
        max-idle: 5
```

### Minio Config
```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: temporary-assets
```

---

## 🎯 **Benefits:**

### 1. **Performance**
- ⚡ Cache HIT = ~5ms (very fast)
- 🐌 Cache MISS = ~200ms (still acceptable)
- 📊 90%+ requests dari cache

### 2. **Resilience**
- 🛡️ Minio sebagai backup/fallback
- 🔄 Kalo API down, masih bisa serve dari Minio
- 💾 Data persistence untuk audit/replay

### 3. **Cost Optimization**
- 💰 Reduce external API calls (save money)
- 📉 Lower database load
- ⚡ Better user experience (faster response)

---

## 🔧 **Advanced Patterns:**

### 1. **Fallback Strategy**
```java
public Mono<Post> findById(Long id) {
    return redis.findById(id)          // Try cache
        .switchIfEmpty(
            minio.findById(id)         // Try storage
                .switchIfEmpty(
                    api.findById(id)   // Finally API
                        .flatMap(post -> 
                            minio.save(post)
                                .then(redis.save(post))
                                .thenReturn(post)
                        )
                )
        );
}
```

### 2. **Write-Through (Sync)**
```java
public Mono<Post> createPost(Post post) {
    return api.save(post)              // Save to API first
        .flatMap(saved -> 
            Mono.when(
                redis.save(saved),     // Cache immediately
                minio.save(saved)      // Backup immediately
            ).thenReturn(saved)
        );
}
```

### 3. **Write-Behind (Async)**
```java
public Mono<Post> createPost(Post post) {
    return api.save(post)
        .doOnSuccess(saved -> {
            // Async, fire-and-forget
            redis.save(saved).subscribe();
            minio.save(saved).subscribe();
        });
}
```

---

## 📊 **Monitoring & Metrics:**

```java
@Component
public class CacheMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordCacheHit() {
        meterRegistry.counter("cache.hits", "layer", "redis").increment();
    }
    
    public void recordCacheMiss() {
        meterRegistry.counter("cache.misses", "layer", "redis").increment();
    }
    
    public void recordStorageSave() {
        meterRegistry.counter("storage.saves", "type", "minio").increment();
    }
}
```

**Metrics to track:**
- Cache hit ratio: `hits / (hits + misses)`
- Average response time
- Storage write latency
- Error rates

---

## ⚠️ **Common Pitfalls:**

### 1. **Cache Stampede**
```java
// Problem: Multiple requests hit cache miss simultaneously
// Solution: Use distributed lock

public Mono<Post> findById(Long id) {
    return redis.findById(id)
        .switchIfEmpty(
            acquireLock(id)  // Only one request fetches
                .flatMap(lock -> 
                    api.findById(id)
                        .flatMap(this::saveAndCache)
                        .doFinally(s -> releaseLock(lock))
                )
        );
}
```

### 2. **Stale Data**
```java
// Solution: Invalidate cache on update
public Mono<Post> updatePost(Long id, Post updated) {
    return api.update(id, updated)
        .flatMap(post -> 
            redis.delete(id)           // Invalidate cache
                .then(minio.save(post))  // Update storage
                .thenReturn(post)
        );
}
```

### 3. **Memory Bloat**
```java
// Solution: Set TTL and eviction policy
redis.opsForValue().set(key, post, 
    Duration.ofMinutes(10)  // Auto-expire
);
```

---

## ✅ **When to Use This Pattern:**

| Scenario | Use? |
|----------|------|
| High read, low write | ✅ Perfect |
| External API rate limited | ✅ Yes |
| Need audit trail | ✅ Yes (Minio) |
| Real-time data critical | ⚠️ Maybe (add TTL) |
| High write volume | ❌ No (use DB) |

---

## 🎓 **Summary:**

```
Redis    = Fast access (cache)
Minio    = Persistence (backup/archive)
API/DB   = Source of truth
Composite = Orchestrate semua tanpa Service tau!
```

**Key Points:**
1. ✅ Service tidak tahu tentang Redis/Minio
2. ✅ Composite adapter handle complexity
3. ✅ Easy to change strategy
4. ✅ Performance + Resilience

Mau implementation lengkap dengan Redis & Minio config? 🚀

