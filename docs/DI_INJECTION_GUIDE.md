# Dependency Injection Guide: Composite Adapter Pattern

> 📖 **Prerequisites:** Read [COMPOSITE_ADAPTER_GUIDE.md](COMPOSITE_ADAPTER_GUIDE.md) first for the pattern overview.

## 🎯 **Masalah:** Gimana cara panggil Redis dan Minio?

**Jawaban:** Pake **Spring Dependency Injection** dengan **@Qualifier**!

---

## 📝 **Step by Step**

### Step 1: Buat Individual Adapters dengan @Qualifier

```java
// 1️⃣ Redis Adapter (Secondary Adapter)
@Component
@Qualifier("redis")  // ← ID: "redis"
public class PostRedisAdapter implements LoadPostPort {
    
    public Mono<Post> loadById(Long id) {
        // Redis logic
    }
    
    public Mono<Void> save(Post post) {
        // Save to Redis
    }
}
```

```java
// 2️⃣ Minio Adapter (Secondary Adapter)
@Component
@Qualifier("minio")  // ← ID: "minio"
public class PostMinioAdapter implements LoadPostPort {
    
    public Mono<Post> loadById(Long id) {
        // Minio logic
    }
    
    public Mono<Void> save(Post post) {
        // Save to Minio
    }
}
```

---

### Step 2: Inject ke Composite Adapter

```java
@Component
@Primary  // ← Ini yang dipake default
public class PostCachedStorageAdapter implements LoadPostPort {
    
    // ✅ Declare dependencies
    private final PostRedisAdapter redisAdapter;
    private final PostMinioAdapter minioAdapter;
    
    // ✅ Constructor Injection dengan @Qualifier
    public PostCachedStorageAdapter(
        @Qualifier("redis") PostRedisAdapter redisAdapter,   // Inject adapter dengan qualifier "redis"
        @Qualifier("minio") PostMinioAdapter minioAdapter    // Inject adapter dengan qualifier "minio"
    ) {
        this.redisAdapter = redisAdapter;
        this.minioAdapter = minioAdapter;
    }
    
    // ✅ Sekarang bisa manggil Redis & Minio!
    @Override
    public Mono<Post> loadById(Long id) {
        return redisAdapter.loadById(id)           // 1. Cek Redis dulu
            .switchIfEmpty(
                minioAdapter.loadById(id)          // 2. Kalo ga ada, ambil dari Minio
                    .flatMap(post -> 
                        redisAdapter.save(post)     // 3. Cache ke Redis
                            .thenReturn(post)
                    )
            );
    }
}
```

---

### Step 3: Service cuma inject LoadPostPort

```java
@Service
public class PostService implements GetPostUseCase {
    
    private final LoadPostPort loadPostPort;  // ✅ Inject Output Port (interface)
    
    // Spring auto-inject PostCachedStorageAdapter karena @Primary
    public PostService(LoadPostPort loadPostPort) {
        this.loadPostPort = loadPostPort;
    }
    
    @Override
    public Mono<Post> getPost(Long id) {
        return loadPostPort.loadById(id);  // Otomatis pake composite adapter!
    }
}
```

> 💡 **Port-based architecture:**
> - Service implements Input Port (GetPostUseCase)
> - Service uses Output Port (LoadPostPort)
> - Spring injects the adapter that implements Output Port

---

## 🔍 **Spring Bean Resolution Flow**

```
1. Service minta: LoadPostPort
   └─> Spring cari bean yang implements LoadPostPort
       ├─ PostRedisAdapter     (@Qualifier("redis"))
       ├─ PostMinioAdapter     (@Qualifier("minio"))
       └─ PostCachedStorageAdapter (@Primary) ← INI YANG DIPILIH!

2. PostCachedStorageAdapter butuh dependencies:
   ├─ @Qualifier("redis") PostRedisAdapter 
   │  └─> Spring inject PostRedisAdapter
   └─ @Qualifier("minio") PostMinioAdapter
      └─> Spring inject PostMinioAdapter

3. Semua dependencies ter-resolve ✅
```

---

## 📊 **Cara Kerja @Qualifier**

### Tanpa @Qualifier (ERROR!)
```java
@Component
public class PostRedisAdapter implements LoadPostPort { }

@Component
public class PostMinioAdapter implements LoadPostPort { }

@Component
public class PostCachedStorageAdapter implements LoadPostPort {
    
    // ❌ ERROR! Spring bingung, LoadPostPort ada 3 implementasi!
    public PostCachedStorageAdapter(
        LoadPostPort redis,   // Mana yang dipilih??
        LoadPostPort minio    // Mana yang dipilih??
    ) { }
}
```

**Error:**
```
No qualifying bean of type 'LoadPostPort' available: 
expected single matching bean but found 3: 
postRedisAdapter, postMinioAdapter, postCachedStorageAdapter
```

---

### Dengan @Qualifier (WORKS!)
```java
@Component
@Qualifier("redis")  // ← Kasih ID
public class PostRedisAdapter implements LoadPostPort { }

@Component
@Qualifier("minio")  // ← Kasih ID
public class PostMinioAdapter implements LoadPostPort { }

@Component
@Primary  // ← Default choice
public class PostCachedStorageAdapter implements LoadPostPort {
    
    // ✅ Spring tahu mana yang mau di-inject!
    public PostCachedStorageAdapter(
        @Qualifier("redis") LoadPostPort redis,   // Pilih yang ID-nya "redis"
        @Qualifier("minio") LoadPostPort minio    // Pilih yang ID-nya "minio"
    ) { }
}
```

---

## 🎯 **Complete Example**

### File Structure
```
infrastructure/
├── cache/
│   └── post/
│       └── PostRedisAdapter.java      (@Qualifier("redis"))
├── storage/
│   └── post/
│       └── PostMinioAdapter.java      (@Qualifier("minio"))
└── composite/
    └── PostCachedStorageAdapter.java  (@Primary)
```

### Code

```java
// 1️⃣ PostRedisAdapter.java
package ...infrastructure.cache.post;

@Component
@Qualifier("redis")
public class PostRedisAdapter implements LoadPostPort {
    
    private final RedisTemplate<String, Post> redisTemplate;
    
    public PostRedisAdapter(RedisTemplate<String, Post> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public Mono<Post> loadById(Long id) {
        String key = "post:" + id;
        return Mono.fromCallable(() -> 
            redisTemplate.opsForValue().get(key)
        );
    }
    
    public Mono<Void> save(Post post) {
        String key = "post:" + post.id();
        return Mono.fromRunnable(() ->
            redisTemplate.opsForValue().set(key, post, Duration.ofMinutes(10))
        );
    }
}
```

```java
// 2️⃣ PostMinioAdapter.java
package ...infrastructure.storage.post;

@Component
@Qualifier("minio")
public class PostMinioAdapter implements LoadPostPort {
    
    private final MinioClient minioClient;
    
    public PostMinioAdapter(MinioClient minioClient) {
        this.minioClient = minioClient;
    }
    
    public Mono<Post> loadById(Long id) {
        return Mono.fromCallable(() -> {
            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket("posts")
                    .object("posts/" + id + ".json")
                    .build()
            );
            return parseJson(stream);
        });
    }
    
    public Mono<Void> save(Post post) {
        return Mono.fromRunnable(() -> {
            String json = toJson(post);
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket("posts")
                    .object("posts/" + post.id() + ".json")
                    .stream(new ByteArrayInputStream(json.getBytes()), -1, 10485760)
                    .build()
            );
        });
    }
}
```

```java
// 3️⃣ PostCachedStorageAdapter.java
package ...infrastructure.composite;

@Component
@Primary  // ← Default implementation
public class PostCachedStorageAdapter implements LoadPostPort {
    
    private final PostRedisAdapter redis;
    private final PostMinioAdapter minio;
    
    // ✅ Spring auto-inject berdasarkan @Qualifier
    public PostCachedStorageAdapter(
        @Qualifier("redis") PostRedisAdapter redis,
        @Qualifier("minio") PostMinioAdapter minio
    ) {
        this.redis = redis;
        this.minio = minio;
    }
    
    @Override
    public Mono<Post> loadById(Long id) {
        return redis.loadById(id)                    // Try cache
            .doOnNext(post -> 
                log.info("Cache HIT for post {}", id)
            )
            .switchIfEmpty(
                Mono.defer(() -> {
                    log.info("Cache MISS for post {}", id);
                    return minio.loadById(id)         // Get from storage
                        .flatMap(post -> 
                            redis.save(post)          // Cache it
                                .thenReturn(post)
                        );
                })
            );
    }
    
    @Override
    public Mono<Void> save(Post post) {
        // Write-through: save to both
        return minio.save(post)                       // Save to storage first
            .then(redis.save(post));                   // Then cache
    }
}
```

```java
// 4️⃣ PostService.java
package ...application.service;

@Service
public class PostService implements GetPostUseCase {
    
    private final LoadPostPort loadPostPort;
    
    // Spring inject PostCachedStorageAdapter (karena @Primary)
    public PostService(LoadPostPort loadPostPort) {
        this.loadPostPort = loadPostPort;
    }
    
    @Override
    public Mono<Post> getPost(Long id) {
        return loadPostPort.loadById(id);  // Magic happens here!
    }
}
```

---

## 🚀 **Flow Execution**

### Request: GET /api/posts/1

```
1. Controller → PostService.getPost(1)
   │
2. PostService → repository.findById(1)
   │  (repository = PostCachedStorageAdapter karena @Primary)
   │
3. PostCachedStorageAdapter.loadById(1)
   │
4. redis.loadById(1)  ← Check Redis cache
   │
   ├─ Cache HIT  → Return Post ✅
   │
   └─ Cache MISS → minio.loadById(1)  ← Get from Minio
                    │
                    ├─ Found → redis.save(post) → Return Post ✅
                    │
                    └─ Not Found → Error ❌
```

---

## 💡 **Alternative: Manual @Bean Configuration**

Kalau ga mau pake @Component, bisa pake @Configuration:

```java
@Configuration
public class AdapterConfig {
    
    @Bean
    @Qualifier("redis")
    public LoadPostPort redisAdapter(RedisTemplate<String, Post> redisTemplate) {
        return new PostRedisAdapter(redisTemplate);
    }
    
    @Bean
    @Qualifier("minio")
    public LoadPostPort minioAdapter(MinioClient minioClient) {
        return new PostMinioAdapter(minioClient);
    }
    
    @Bean
    @Primary
    public LoadPostPort compositeAdapter(
        @Qualifier("redis") LoadPostPort redis,
        @Qualifier("minio") LoadPostPort minio
    ) {
        return new PostCachedStorageAdapter(
            (PostRedisAdapter) redis,
            (PostMinioAdapter) minio
        );
    }
}
```

> 💡 **See:** [COMPOSITE_ADAPTER_GUIDE.md](COMPOSITE_ADAPTER_GUIDE.md) for composite pattern details.

---

## ✅ **Summary**

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@Component` | Register as Spring bean | All adapters |
| `@Qualifier("name")` | Give bean an ID | `@Qualifier("redis")` |
| `@Primary` | Default when multiple beans | Composite adapter |
| Constructor Injection | Inject dependencies | `PostCachedStorageAdapter(...)` |

**Key Points:**
1. ✅ Individual adapters: `@Component` + `@Qualifier`
2. ✅ Composite adapter: `@Component` + `@Primary`
3. ✅ Inject dengan: `@Qualifier("name")` di constructor
4. ✅ Spring auto-wire semua!

**Result:** Service ga tahu Redis/Minio, tapi tetep bisa pake! 🎉

---

## 📚 Related Guides

- **[PURE_HEXAGONAL_ARCHITECTURE.md](PURE_HEXAGONAL_ARCHITECTURE.md)** - Main architecture guide
- **[COMPOSITE_ADAPTER_GUIDE.md](COMPOSITE_ADAPTER_GUIDE.md)** - Composite pattern overview
- **[HEXAGONAL_MAPPING_GUIDE.md](HEXAGONAL_MAPPING_GUIDE.md)** - Mapping strategy

