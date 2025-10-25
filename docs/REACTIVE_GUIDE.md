# Reactive Programming Guide dengan Spring WebFlux

> 📖 **Context:** This guide explains reactive programming concepts used throughout the hexagonal architecture.  
> See [PURE_HEXAGONAL_ARCHITECTURE.md](PURE_HEXAGONAL_ARCHITECTURE.md) for how reactive code fits in the architecture.

## Daftar Isi
1. [Konsep Dasar](#konsep-dasar)
2. [Creation Operators](#creation-operators)
3. [Transformation Operators](#transformation-operators)
4. [Filtering Operators](#filtering-operators)
5. [Error Handling](#error-handling)
6. [Timing Operators](#timing-operators)
7. [Combining Operators](#combining-operators)
8. [Conditional Operators](#conditional-operators)
9. [Threading/Scheduling](#threadingscheduling)
10. [Backpressure](#backpressure)
11. [Utility Operators](#utility-operators)
12. [Best Practices](#best-practices)

## Konsep Dasar

### Mono vs Flux - Analogi Sederhana

**Mono** = **Kotak yang bisa kosong atau berisi 1 barang**
- `Mono<String>` = kotak yang bisa kosong atau berisi 1 string
- `Mono<User>` = kotak yang bisa kosong atau berisi 1 user

**Flux** = **Keranjang yang bisa kosong atau berisi banyak barang**
- `Flux<String>` = keranjang yang bisa kosong atau berisi banyak string
- `Flux<User>` = keranjang yang bisa kosong atau berisi banyak user

### Contoh Praktis Mono vs Flux

```java
// MONO - 1 hasil
Mono<String> nama = Mono.just("John");           // Kotak berisi "John"
Mono<String> kosong = Mono.empty();              // Kotak kosong
Mono<String> error = Mono.error(new Exception()); // Kotak rusak

// FLUX - Banyak hasil
Flux<String> namaNama = Flux.just("John", "Jane", "Bob"); // Keranjang berisi 3 nama
Flux<Integer> angka = Flux.range(1, 5);                   // Keranjang berisi 1,2,3,4,5
Flux<String> kosong = Flux.empty();                       // Keranjang kosong
```

### Kapan Pakai Mono vs Flux?

**Pakai MONO untuk:**
- 1 hasil (get user by ID)
- 0 atau 1 hasil (find user by email)
- Operasi yang return 1 value (save, update, delete)
- **REST API** kebanyakan return 1 object → pakai **Mono**

**Pakai FLUX untuk:**
- Banyak hasil (get all users, get posts by user)
- Stream data (real-time updates)
- List/Collection results
- **Streaming/SSE** return banyak objects → pakai **Flux**

### Contoh di Controller

```java
// MONO - 1 post (paling umum di REST API)
@GetMapping("/posts/{id}")
public Mono<Post> getPost(@PathVariable Long id) {
    return webClient.get().uri("/posts/{id}", id)
        .retrieve().bodyToMono(Post.class); // ← bodyToMono
}

// FLUX - banyak posts
@GetMapping("/posts")
public Flux<Post> getAllPosts() {
    return webClient.get().uri("/posts")
        .retrieve().bodyToFlux(Post.class); // ← bodyToFlux
}

// MONO - 0 atau 1 post (bisa kosong)
@GetMapping("/posts/search")
public Mono<Post> findPost(@RequestParam String title) {
    return webClient.get().uri("/posts")
        .retrieve().bodyToFlux(Post.class)
        .filter(post -> post.getTitle().contains(title))
        .next(); // Ambil yang pertama, jadi Mono
}
```

### Konversi Mono ↔ Flux

```java
// Flux ke Mono
Flux<String> flux = Flux.just("a", "b", "c");
Mono<String> mono = flux.next();        // Ambil yang pertama
Mono<List<String>> monoList = flux.collectList(); // Jadikan list

// Mono ke Flux
Mono<String> mono = Mono.just("hello");
Flux<String> flux = mono.flux();        // Jadikan Flux dengan 1 elemen
```

### Mono vs Flux: Perbedaan dan Penggunaan

#### Perbedaan inti
- **Mono<T>**: merepresentasikan paling banyak 1 item (0 atau 1) atau error/complete.
- **Flux<T>**: merepresentasikan 0 hingga N item (termasuk tak hingga) atau error/complete.
- Implikasi API:
  - Mono cocok untuk operasi CRUD yang mengembalikan satu entitas/hasil.
  - Flux cocok untuk koleksi/list atau streaming (SSE, batch besar, event).

#### Usage matrix (ringkas)
- 1 entity (GET by id) → Mono<T>
- Create/Update/Delete → Mono<T> atau Mono<Void>
- List/paginated results → Flux<T> (atau Mono<Page<T>> jika pagination server-side)
- Streaming/SSE → Flux<T> dengan `TEXT_EVENT_STREAM`
- Aggregasi ke list di akhir → Flux<T>.collectList() → Mono<List<T>>

#### Contoh yang kontras
```java
// Mono: satu item
@GetMapping("/users/{id}")
public Mono<User> getUser(@PathVariable Long id) {
    return webClient.get().uri("/users/{id}", id)
        .retrieve().bodyToMono(User.class);
}
// Response: {"id": 1, "name": "John", "email": "john@example.com"}

// Flux: banyak item (streaming dari upstream)
@GetMapping("/users")
public Flux<User> getUsers() {
    return webClient.get().uri("/users")
        .retrieve().bodyToFlux(User.class);
}
// Response: [{"id": 1, "name": "John"}, {"id": 2, "name": "Jane"}, {"id": 3, "name": "Bob"}]

// Aggregasi: butuh List di response → kumpulkan di akhir
@GetMapping("/users/list")
public Mono<List<User>> getUsersAsList() {
    return webClient.get().uri("/users")
        .retrieve().bodyToFlux(User.class)
        .collectList(); // Mono<List<User>>
}
// Response: [{"id": 1, "name": "John"}, {"id": 2, "name": "Jane"}, {"id": 3, "name": "Bob"}]
```

#### Contoh data konkret
```java
// Mono - 1 object
Mono<User> user = Mono.just(new User(1L, "John", "john@example.com"));
// Data: User{id=1, name="John", email="john@example.com"}

// Flux - banyak objects
Flux<User> users = Flux.just(
    new User(1L, "John", "john@example.com"),
    new User(2L, "Jane", "jane@example.com"),
    new User(3L, "Bob", "bob@example.com")
);
// Data: [User{id=1, name="John"}, User{id=2, name="Jane"}, User{id=3, name="Bob"}]

// Mono.empty() - kosong
Mono<User> emptyUser = Mono.empty();
// Data: null (tidak ada data)

// Flux.empty() - kosong
Flux<User> emptyUsers = Flux.empty();
// Data: [] (array kosong)
```

#### Anti‑pattern umum
- Menggunakan Flux untuk satu item: gunakan Mono agar kontrak jelas dan validasi lebih mudah.
- Mengumpulkan Flux besar menjadi List tanpa batas: gunakan pagination atau backpressure; jika perlu list, pertimbangkan batas ukuran.
- Melakukan blocking dalam pipeline (JDBC, file I/O): offload ke `Schedulers.boundedElastic()` atau pakai driver reactive (R2DBC).

### Blocking vs Non-Blocking
- **Blocking**: Thread menunggu sampai operasi selesai
- **Non-Blocking**: Thread tidak menunggu, melanjutkan kerja lain

## Creation Operators

### Membuat Mono/Flux dari nilai

```java
// Mono
Mono.just("Hello")                    // Single value
Mono.empty()                          // Empty Mono
Mono.error(new RuntimeException())    // Error Mono

// Flux
Flux.just(1, 2, 3)                    // Multiple values
Flux.range(1, 10)                     // Range 1-10
Flux.interval(Duration.ofSeconds(1))  // Emit every 1 second
```

### Use Cases
- **Mono.just()**: Return static data, default values
- **Flux.range()**: Generate test data, pagination
- **Flux.interval()**: Heartbeat, periodic updates

### Contoh Implementasi
```java
@GetMapping("/static-data")
public Mono<String> getStaticData() {
    return Mono.just("Static response");
}

@GetMapping("/test-data")
public Flux<Integer> getTestData() {
    return Flux.range(1, 100);
}
```

## Transformation Operators

### map vs flatMap vs concatMap vs switchMap

```java
// map: Transform 1:1 (sync)
.map(item -> item.toUpperCase())

// flatMap: Transform 1:N (async, parallel)
.flatMap(item -> webClient.get().uri("/api/{id}", item).retrieve().bodyToMono(String.class))

// concatMap: Transform 1:N (async, sequential)
.concatMap(item -> webClient.get().uri("/api/{id}", item).retrieve().bodyToMono(String.class))

// switchMap: Transform 1:N (cancel previous)
.switchMap(item -> webClient.get().uri("/api/{id}", item).retrieve().bodyToMono(String.class))
```

### bodyToMono vs bodyToFlux

```java
// bodyToMono: untuk 1 object (paling umum di REST API)
.retrieve().bodyToMono(User.class)        // Return 1 user
.retrieve().bodyToMono(String.class)      // Return 1 string

// bodyToFlux: untuk banyak objects
.retrieve().bodyToFlux(User.class)        // Return banyak users
.retrieve().bodyToFlux(Post.class)        // Return banyak posts
```

**Kapan pakai bodyToMono?**
- GET `/users/{id}` → return 1 user
- POST `/users` → create 1 user
- PUT `/users/{id}` → update 1 user
- DELETE `/users/{id}` → delete 1 user

**Kapan pakai bodyToFlux?**
- GET `/users` → return banyak users
- GET `/users/{id}/posts` → return banyak posts
- Streaming endpoints

### Use Cases
- **map**: Transformasi ringan (parsing, rename field)
- **flatMap**: I/O async paralel (multiple API calls)
- **concatMap**: I/O async tapi urutan penting
- **switchMap**: Search autocomplete, debounce

### Contoh Implementasi
```java
@GetMapping("/posts/{id}/details")
public Mono<PostDetails> getPostDetails(@PathVariable Long id) {
    return webClient
        .get()
        .uri("/posts/{id}", id)
        .retrieve()
        .bodyToMono(Post.class)
        .flatMap(post -> 
            webClient.get()
                .uri("/users/{userId}", post.getUserId())
                .retrieve()
                .bodyToMono(User.class)
                .map(user -> new PostDetails(post, user))
        );
}

@GetMapping("/posts/batch")
public Flux<Post> getPostsBatch(@RequestParam List<Long> ids) {
    return Flux.fromIterable(ids)
        .flatMap(id -> webClient.get().uri("/posts/{id}", id).retrieve().bodyToMono(Post.class), 5); // concurrency 5
}
```

## Filtering Operators

```java
.filter(item -> item.length() > 5)     // Filter berdasarkan kondisi
.take(5)                              // Ambil 5 item pertama
.takeLast(3)                          // Ambil 3 item terakhir
.skip(2)                              // Skip 2 item pertama
.distinct()                           // Hapus duplikat
.distinctUntilChanged()               // Hapus duplikat berturut-turut
```

### Use Cases
- **filter**: Validasi data, business rules
- **take**: Pagination, limit results
- **distinct**: Remove duplicates
- **skip**: Pagination offset

### Contoh Implementasi
```java
@GetMapping("/posts/valid")
public Flux<Post> getValidPosts() {
    return webClient
        .get()
        .uri("/posts")
        .retrieve()
        .bodyToFlux(Post.class)
        .filter(post -> post.getTitle() != null && !post.getTitle().isEmpty())
        .take(10);
}
```

## Error Handling

```java
.onErrorReturn("default")                                    // Return default jika error
.onErrorResume(throwable -> Mono.just("fallback"))          // Return fallback
.onErrorMap(throwable -> new CustomException())             // Transform error
.retry(3)                                                    // Retry 3 kali
.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))         // Retry dengan backoff
.doOnError(error -> log.error("Error occurred: {}", error)) // Log error
```

### Use Cases
- **onErrorReturn**: Fallback untuk data kosong
- **retry**: Network resilience
- **onErrorResume**: Alternative data source

### Contoh Implementasi
```java
@GetMapping("/posts/{id}/safe")
public Mono<Post> getPostSafe(@PathVariable Long id) {
    return webClient
        .get()
        .uri("/posts/{id}", id)
        .retrieve()
        .bodyToMono(Post.class)
        .timeout(Duration.ofSeconds(3))
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
        .onErrorReturn(Post.empty())
        .doOnError(error -> log.error("Failed to fetch post {}: {}", id, error.getMessage()));
}
```

## Timing Operators

```java
.delay(Duration.ofSeconds(2))          // Delay sebelum emit
.timeout(Duration.ofSeconds(5))        // Timeout setelah 5s
.delayElements(Duration.ofMillis(100)) // Delay antar element
```

### Use Cases
- **delay**: Rate limiting, simulation
- **timeout**: Prevent hanging requests
- **delayElements**: Throttling, backpressure

### Contoh Implementasi
```java
@GetMapping("/posts/stream")
public Flux<Post> streamPosts() {
    return webClient
        .get()
        .uri("/posts")
        .retrieve()
        .bodyToFlux(Post.class)
        .delayElements(Duration.ofMillis(500)) // Emit every 500ms
        .take(10);
}
```

## Combining Operators

```java
// Combine multiple streams
Mono.zip(mono1, mono2, (a, b) -> a + b)                    // Combine 2 Monos
Flux.merge(flux1, flux2)                                   // Merge streams (parallel)
Flux.concat(flux1, flux2)                                 // Concat streams (sequential)
Flux.combineLatest(flux1, flux2, (a, b) -> a + b)         // Latest from each
```

### Use Cases
- **zip**: Combine related data
- **merge**: Aggregate multiple sources
- **concat**: Sequential processing

### Contoh Implementasi
```java
@GetMapping("/user/{id}/profile")
public Mono<UserProfile> getUserProfile(@PathVariable Long id) {
    Mono<User> userMono = webClient.get().uri("/users/{id}", id).retrieve().bodyToMono(User.class);
    Mono<List<Post>> postsMono = webClient.get().uri("/users/{id}/posts", id).retrieve().bodyToFlux(Post.class).collectList();
    
    return Mono.zip(userMono, postsMono)
        .map(tuple -> new UserProfile(tuple.getT1(), tuple.getT2()));
}
```

## Conditional Operators

```java
.switchIfEmpty(Mono.just("default"))   // Jika empty, pakai default
.defaultIfEmpty("default")             // Default value jika empty
.takeUntil(item -> item.equals("stop")) // Ambil sampai kondisi terpenuhi
.takeWhile(item -> item < 10)           // Ambil selama kondisi terpenuhi
```

### Use Cases
- **switchIfEmpty**: Fallback data
- **takeUntil**: Stop condition
- **takeWhile**: Continue condition

### Contoh Implementasi
```java
@GetMapping("/posts/{id}/with-fallback")
public Mono<Post> getPostWithFallback(@PathVariable Long id) {
    return webClient
        .get()
        .uri("/posts/{id}", id)
        .retrieve()
        .bodyToMono(Post.class)
        .switchIfEmpty(
            webClient.get()
                .uri("/posts/fallback/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
        );
}
```

## Threading/Scheduling

```java
.subscribeOn(Schedulers.parallel())        // Execute di thread pool parallel
.subscribeOn(Schedulers.boundedElastic()) // Execute di thread pool elastic
.publishOn(Schedulers.single())           // Switch ke thread single
```

### Use Cases
- **parallel**: CPU-intensive tasks
- **boundedElastic**: Blocking I/O (JDBC, file operations)
- **single**: UI updates, sequential processing

### Contoh Implementasi
```java
@GetMapping("/posts/{id}/heavy-processing")
public Mono<ProcessedPost> getProcessedPost(@PathVariable Long id) {
    return webClient
        .get()
        .uri("/posts/{id}", id)
        .retrieve()
        .bodyToMono(Post.class)
        .subscribeOn(Schedulers.parallel()) // CPU-intensive processing
        .map(post -> heavyProcessing(post));
}
```

## Backpressure

```java
.onBackpressureBuffer(100)                // Buffer sampai 100 items
.onBackpressureDrop()                     // Drop items jika buffer penuh
.onBackpressureLatest()                   // Keep latest item
```

### Use Cases
- **buffer**: Handle burst traffic
- **drop**: Prioritize latest data
- **latest**: Real-time updates

## Utility Operators

```java
.doOnNext(item -> log.info("Next: {}", item))     // Side effect
.doOnError(error -> log.error("Error: {}", error)) // Side effect on error
.doOnComplete(() -> log.info("Complete"))          // Side effect on complete
.doOnSubscribe(subscription -> log.info("Subscribed")) // Side effect on subscribe
.cache()                                          // Cache hasil
.repeat(3)                                        // Repeat 3 kali
```

### Use Cases
- **doOnNext**: Logging, metrics
- **cache**: Expensive operations
- **repeat**: Retry logic

### Contoh Implementasi
```java
@GetMapping("/posts/{id}/cached")
public Mono<Post> getCachedPost(@PathVariable Long id) {
    return webClient
        .get()
        .uri("/posts/{id}", id)
        .retrieve()
        .bodyToMono(Post.class)
        .doOnNext(post -> log.info("Fetched post: {}", post.getId()))
        .cache(Duration.ofMinutes(5)) // Cache for 5 minutes
        .doOnSubscribe(sub -> log.info("Subscribed to post: {}", id));
}
```

## Best Practices

### 1. Pilih Operator yang Tepat
- Gunakan `map` untuk transformasi ringan
- Gunakan `flatMap` untuk I/O async
- Gunakan `concatMap` jika urutan penting

### 2. Error Handling
- Selalu handle error dengan `onErrorReturn` atau `onErrorResume`
- Gunakan `retry` untuk network resilience
- Log error dengan `doOnError`

### 3. Performance
- Batasi concurrency dengan `flatMap(fn, concurrency)`
- Gunakan `cache` untuk operasi mahal
- Hindari blocking operations di event loop

### 4. Testing
- Gunakan `StepVerifier` untuk test reactive streams
- Test error scenarios
- Test backpressure

### 5. **BlockHound - Detect Blocking Calls** 🔍

**BlockHound** adalah Java agent untuk detect blocking calls dalam reactive code.

#### **Why Use BlockHound?**
- ✅ Detect blocking operations yang accidentally masuk ke reactive pipeline
- ✅ Throw error immediately saat ada blocking call
- ✅ Memastikan code benar-benar non-blocking

#### **What BlockHound Detects:**
```java
// ❌ BAD: Blocking operations
Thread.sleep(1000);              // Blocking!
Object.wait();                   // Blocking!
FileInputStream.read();          // Blocking I/O!
synchronized(lock) { ... }       // Blocking!
jdbcTemplate.query(...);         // Blocking JDBC!
```

#### **Installation:**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.projectreactor.tools</groupId>
    <artifactId>blockhound</artifactId>
    <version>1.0.8.RELEASE</version>
</dependency>
```

#### **Configuration:**
```java
// config/BlockHoundConfig.java
@Configuration
public class BlockHoundConfig {
    
    @PostConstruct
    public void init() {
        // Only enable in DEV/TEST (not PROD)
        BlockHound.install(builder -> {
            // Allow specific blocking calls if needed
            builder.allowBlockingCallsInside(
                "com.fasterxml.jackson.databind.ObjectMapper",
                "readValue"
            );
        });
    }
}
```

#### **Example: BlockHound Catches Violation**
```java
// ❌ This will THROW BlockingOperationError
@GetMapping("/bad")
public Mono<String> badEndpoint() {
    return Mono.fromCallable(() -> {
        Thread.sleep(1000);  // 💥 BlockHound throws error!
        return "result";
    });
}

// ✅ Correct: Non-blocking
@GetMapping("/good")
public Mono<String> goodEndpoint() {
    return Mono.delay(Duration.ofSeconds(1))
        .map(tick -> "result");
}
```

#### **Common Mistakes BlockHound Catches:**

**1. Thread.sleep() in Reactive Chain:**
```java
// ❌ WRONG
return Mono.fromCallable(() -> {
    Thread.sleep(1000);
    return "data";
});

// ✅ CORRECT
return Mono.delay(Duration.ofSeconds(1))
    .map(tick -> "data");
```

**2. Blocking I/O:**
```java
// ❌ WRONG: FileInputStream is blocking
return Mono.fromCallable(() -> {
    FileInputStream fis = new FileInputStream("file.txt");
    return fis.read();
});

// ✅ CORRECT: Use reactive file operations
return DataBufferUtils.read(
    new FileSystemResource("file.txt"),
    dataBufferFactory,
    4096
);
```

**3. JDBC in Reactive:**
```java
// ❌ WRONG: JDBC is blocking
return Mono.fromCallable(() -> {
    return jdbcTemplate.queryForObject("SELECT * FROM users WHERE id = ?", ...);
});

// ✅ CORRECT: Use R2DBC
return r2dbcTemplate.queryForObject("SELECT * FROM users WHERE id = ?", ...);
```

**4. Synchronized Blocks:**
```java
// ❌ WRONG: synchronized is blocking
return Mono.fromCallable(() -> {
    synchronized(lock) {
        return sharedResource.getData();
    }
});

// ✅ CORRECT: Use reactive locks or atomic operations
return Mono.fromCallable(() -> 
    atomicReference.get()
);
```

#### **How to Fix Blocking Operations:**

| Blocking | Non-Blocking Alternative |
|----------|-------------------------|
| `Thread.sleep()` | `Mono.delay()` |
| `FileInputStream` | `DataBufferUtils` |
| `JDBC` | `R2DBC` |
| `RestTemplate` | `WebClient` |
| `synchronized` | `AtomicReference`, `Mutex` |
| `ExecutorService.submit()` | `Schedulers.boundedElastic()` |

#### **When to Offload Blocking Operations:**

Jika **tidak bisa** avoid blocking (legacy code, third-party lib):
```java
// Use boundedElastic scheduler
return Mono.fromCallable(() -> {
    // Blocking operation here is OK
    return legacyBlockingService.getData();
})
.subscribeOn(Schedulers.boundedElastic());  // ✅ Offload to separate thread pool
```

#### **BlockHound Best Practices:**
1. ✅ Enable only in DEV/TEST (not PROD - performance overhead)
2. ✅ Run tests with BlockHound enabled
3. ✅ Fix violations immediately
4. ✅ Document allowed blocking calls with `allowBlockingCallsInside()`
5. ✅ Use `Schedulers.boundedElastic()` for unavoidable blocking

#### **Environment Setup:**
```properties
# application-dev.properties
spring.profiles.active=dev
# BlockHound will be enabled

# application-prod.properties
spring.profiles.active=prod
# BlockHound will be disabled
```

> 💡 **Tip:** Run your test suite with BlockHound to catch all blocking violations before production!

### Contoh Testing
```java
@Test
void testGetPost() {
    StepVerifier.create(controller.getPost(1L))
        .expectNextMatches(post -> post.getId().equals(1L))
        .verifyComplete();
}

@Test
void testGetPostError() {
    StepVerifier.create(controller.getPost(999L))
        .expectError(PostNotFoundException.class)
        .verify();
}

@Test
void testNoBlockingCalls() {
    // BlockHound will catch any blocking operations
    StepVerifier.create(service.getPost(1L))
        .expectNextCount(1)
        .verifyComplete();
    // If blocking detected → BlockingOperationError thrown!
}
```

## Kesimpulan

### Ringkasan Mono vs Flux
- **Mono** = 0 atau 1 object → `bodyToMono()` → **Paling umum di REST API**
- **Flux** = 0 hingga N objects → `bodyToFlux()` → **Untuk list/stream**

### Pattern Umum
```java
// 1 object → Mono
@GetMapping("/users/{id}")
public Mono<User> getUser(@PathVariable Long id) {
    return webClient.get().uri("/users/{id}", id)
        .retrieve().bodyToMono(User.class);
}

// Banyak objects → Flux  
@GetMapping("/users")
public Flux<User> getUsers() {
    return webClient.get().uri("/users")
        .retrieve().bodyToFlux(User.class);
}
```

### Reactive programming dengan Spring WebFlux memberikan:
- **Non-blocking I/O**: Better resource utilization
- **Backpressure**: Handle high load gracefully
- **Composable**: Chain operations easily
- **Resilient**: Built-in error handling and retry

### Quick Decision Guide
- **1 object** → `Mono` + `bodyToMono()`
- **Banyak objects** → `Flux` + `bodyToFlux()`
- **Transform ringan** → `map()`
- **I/O async** → `flatMap()`
- **Error handling** → `onErrorReturn()` atau `retry()`

Pilih operator berdasarkan use case dan selalu test error scenarios!

---

## 📚 How Reactive Fits in Hexagonal Architecture

### **Reactive in Adapters:**
```java
// Secondary Adapter (API Client)
@Component
public class PostApiClient implements LoadPostPort {
    
    @Override
    public Mono<Post> loadById(Long id) {
        return webClient.get()
            .uri("/posts/{id}", id)
            .retrieve()
            .bodyToMono(PostDto.class)  // ✅ Reactive here
            .map(this::toDomain);
    }
}
```

### **Reactive in Services:**
```java
// Service (Application Layer)
@Service
public class PostService implements GetPostUseCase {
    
    @Override
    public Mono<Post> getPost(Long id) {
        return loadPostPort.loadById(id)  // ✅ Reactive chain
            .filter(Post::isValid)
            .switchIfEmpty(Mono.error(...));
    }
}
```

### **Reactive in Controllers:**
```java
// Primary Adapter (HTTP Controller)
@RestController
public class PostController {
    
    @GetMapping("/{id}")
    public Mono<PostResponse> getPost(@PathVariable Long id) {
        return getPostUseCase.getPost(id)  // ✅ Non-blocking
            .map(PostResponse::from);
    }
}
```

> 💡 **See:**
> - [PURE_HEXAGONAL_ARCHITECTURE.md](PURE_HEXAGONAL_ARCHITECTURE.md) - Architecture overview
> - [HEXAGONAL_MAPPING_GUIDE.md](HEXAGONAL_MAPPING_GUIDE.md) - Where mapping happens in reactive chains
