# Simple Pattern: Redis → Database

## 🎯 **Flow:**

```
GET /api/posts/1

Step 1: Check Redis
   ├─ HIT?  → Return (5ms) ✅
   │
   └─ MISS? → Continue to Step 2

Step 2: Get from Database
   └─ Query: SELECT * FROM posts WHERE id = 1

Step 3: Cache to Redis
   └─ SET post:1 <data> EX 600 (10 min TTL)

Step 4: Return data
```

---

## 💻 **Implementation (All in Comments)**

File: `PostCachedDbAdapter.java`

### **Dependencies:**
```java
// Redis untuk cache
private final RedisTemplate<String, Post> redisTemplate;

// Database (JPA)
private final PostJpaRepository jpaRepository;
```

### **Main Method:**
```java
@Override
public Mono<Post> findById(Long id) {
    String cacheKey = "post:" + id;
    
    // 1. Check Redis
    Post cached = redisTemplate.opsForValue().get(cacheKey);
    
    if (cached != null) {
        // CACHE HIT!
        return Mono.just(cached);
    }
    
    // 2. CACHE MISS - Get from DB
    Optional<PostEntity> entityOpt = jpaRepository.findById(id);
    Post post = convertToPost(entityOpt.get());
    
    // 3. Cache it
    redisTemplate.opsForValue().set(
        cacheKey, 
        post, 
        Duration.ofMinutes(10)  // TTL
    );
    
    // 4. Return
    return Mono.just(post);
}
```

---

## 📊 **Performance:**

| Request | Cache Status | Time |
|---------|-------------|------|
| 1st request | MISS | ~50ms (database query) |
| 2nd request | HIT | ~5ms (from Redis) |
| 3rd request | HIT | ~5ms (from Redis) |
| After 10 min | MISS | ~50ms (TTL expired) |

**Result:** 10x faster untuk cached requests! 🚀

---

## 🔧 **Setup Required:**

### 1. **Add Dependencies** (pom.xml)
```xml
<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- PostgreSQL Driver (or MySQL, etc) -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- Spring Data Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2. **Configuration** (application.properties)
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=user
spring.datasource.password=pass
spring.jpa.hibernate.ddl-auto=update

# Redis
spring.redis.host=localhost
spring.redis.port=6379
```

### 3. **Create JPA Entity**
```java
@Entity
@Table(name = "posts")
public class PostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String body;
    
    // Getters & Setters
}
```

### 4. **Create JPA Repository**
```java
public interface PostJpaRepository extends JpaRepository<PostEntity, Long> {
    // Spring Data JPA auto-provides:
    // - findById(Long id)
    // - findAll()
    // - save(PostEntity entity)
    // - deleteById(Long id)
}
```

---

## ✅ **Steps to Activate:**

1. Uncomment code di `PostCachedDbAdapter.java`
2. Uncomment `@Component` dan `@Primary`
3. Create `PostEntity` dan `PostJpaRepository`
4. Add dependencies ke `pom.xml`
5. Configure database & Redis di `application.properties`
6. Run!

---

## 🎓 **Key Points:**

1. ✅ Service **tidak berubah** - still cuma tahu interface
2. ✅ Controller **tidak berubah** - still return DTO
3. ✅ **Only adapter changes** - magic of Hexagonal!
4. ✅ **10x faster** for cached data
5. ✅ **Automatic TTL** - cache expires after 10 min

---

## 🔄 **Current Flow (API Only):**

```
Request → PostService → PostRepository
                            ↓
                       PostApiClient
                            ↓
                    JSONPlaceholder API
```

## 🔄 **Future Flow (Redis + DB):**

```
Request → PostService → PostRepository
                            ↓
                    PostCachedDbAdapter
                            ↓
                    ┌──────┴──────┐
                    │             │
                  Redis        Database
                 (cache)     (PostgreSQL)
```

**Service ga tau bedanya!** 🪄

