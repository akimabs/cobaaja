# 🔍 BlockHound - Detect Blocking Calls in Reactive Code

## 🎯 What is BlockHound?

**BlockHound** adalah Java agent yang detect blocking calls dalam reactive code dan throw error kalau ada blocking operation.

### **Problem:**
Reactive code harus **non-blocking**, tapi mudah accidentally introduce blocking calls:
```java
// ❌ Looks reactive but BLOCKING!
return Mono.fromCallable(() -> {
    Thread.sleep(1000);  // 💥 BLOCKING!
    return "result";
});
```

### **Solution:**
BlockHound catches this immediately:
```
reactor.blockhound.BlockingOperationError: Blocking call! 
  java.lang.Thread.sleep
```

---

## 📦 Installation

### **1. Add Dependency**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.projectreactor.tools</groupId>
    <artifactId>blockhound</artifactId>
    <version>1.0.8.RELEASE</version>
</dependency>
```

### **2. Create Configuration**
```java
// config/BlockHoundConfig.java
package com.loginservice.app.config;

import org.springframework.context.annotation.Configuration;
import reactor.blockhound.BlockHound;
import jakarta.annotation.PostConstruct;

@Configuration
public class BlockHoundConfig {

    @PostConstruct
    public void init() {
        // Only enable in DEV/TEST (not PROD)
        String profile = System.getProperty("spring.profiles.active", "dev");
        
        if (!profile.equals("prod")) {
            BlockHound.install(builder -> {
                // Allow specific blocking calls if needed
                builder.allowBlockingCallsInside(
                    "com.fasterxml.jackson.databind.ObjectMapper",
                    "readValue"
                );
                
                System.out.println("🔍 BlockHound ENABLED");
            });
        }
    }
}
```

### **3. Run Application**
```bash
mvn spring-boot:run
```

Output:
```
🔍 BlockHound ENABLED - Monitoring for blocking calls...
⚠️  Any blocking operation will throw BlockingOperationError
```

---

## 🚨 What BlockHound Detects

### **Common Blocking Operations:**

| Operation | Blocking? | Alternative |
|-----------|-----------|-------------|
| `Thread.sleep()` | ❌ YES | `Mono.delay()` |
| `Object.wait()` | ❌ YES | `Mono.defer()` |
| `FileInputStream` | ❌ YES | `DataBufferUtils` |
| `JDBC calls` | ❌ YES | `R2DBC` |
| `synchronized` | ❌ YES | `AtomicReference` |
| `RestTemplate` | ❌ YES | `WebClient` |
| `Mono.delay()` | ✅ NO | Use this! |
| `WebClient` | ✅ NO | Already non-blocking |

---

## 🛠️ Common Mistakes & Fixes

### **1. Thread.sleep()**

❌ **WRONG:**
```java
@GetMapping("/delay")
public Mono<String> delayEndpoint() {
    return Mono.fromCallable(() -> {
        Thread.sleep(1000);  // 💥 BlockingOperationError!
        return "result";
    });
}
```

✅ **CORRECT:**
```java
@GetMapping("/delay")
public Mono<String> delayEndpoint() {
    return Mono.delay(Duration.ofSeconds(1))
        .map(tick -> "result");
}
```

---

### **2. Blocking I/O (File Reading)**

❌ **WRONG:**
```java
public Mono<String> readFile() {
    return Mono.fromCallable(() -> {
        FileInputStream fis = new FileInputStream("data.txt");  // 💥 Blocking I/O!
        return new String(fis.readAllBytes());
    });
}
```

✅ **CORRECT:**
```java
public Mono<String> readFile() {
    return DataBufferUtils.read(
            new FileSystemResource("data.txt"),
            dataBufferFactory,
            4096
        )
        .map(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            return new String(bytes);
        })
        .collectList()
        .map(list -> String.join("", list));
}
```

---

### **3. JDBC Database Calls**

❌ **WRONG:**
```java
public Mono<User> getUser(Long id) {
    return Mono.fromCallable(() -> 
        jdbcTemplate.queryForObject(  // 💥 Blocking JDBC!
            "SELECT * FROM users WHERE id = ?",
            new Object[]{id},
            User.class
        )
    );
}
```

✅ **CORRECT:**
```java
// Use R2DBC (Reactive Database Connectivity)
public Mono<User> getUser(Long id) {
    return r2dbcTemplate.queryForObject(
        "SELECT * FROM users WHERE id = ?",
        User.class,
        id
    );
}
```

---

### **4. Synchronized Blocks**

❌ **WRONG:**
```java
private Object sharedResource = new Object();

public Mono<String> accessShared() {
    return Mono.fromCallable(() -> {
        synchronized(sharedResource) {  // 💥 Blocking!
            return sharedResource.toString();
        }
    });
}
```

✅ **CORRECT:**
```java
private final AtomicReference<String> atomicResource = new AtomicReference<>("value");

public Mono<String> accessShared() {
    return Mono.fromCallable(() -> 
        atomicResource.get()  // ✅ Non-blocking
    );
}
```

---

### **5. RestTemplate (Old HTTP Client)**

❌ **WRONG:**
```java
public Mono<Post> getPost(Long id) {
    return Mono.fromCallable(() ->
        restTemplate.getForObject(  // 💥 Blocking!
            "https://api.example.com/posts/" + id,
            Post.class
        )
    );
}
```

✅ **CORRECT:**
```java
public Mono<Post> getPost(Long id) {
    return webClient.get()  // ✅ Non-blocking
        .uri("/posts/{id}", id)
        .retrieve()
        .bodyToMono(Post.class);
}
```

---

## 🔧 When You MUST Use Blocking Code

Kadang unavoidable (legacy code, third-party lib yang blocking). Solusi: **Offload ke separate thread pool**

### **Use `Schedulers.boundedElastic()`**

```java
@Component
public class LegacyAdapter implements LoadUserPort {
    
    private final LegacyBlockingService legacyService;
    
    @Override
    public Mono<User> loadById(Long id) {
        return Mono.fromCallable(() -> {
            // ⚠️ Legacy blocking call
            return legacyService.getUser(id);  // JDBC, File I/O, etc
        })
        .subscribeOn(Schedulers.boundedElastic());  // ✅ Offload to blocking thread pool
    }
}
```

**How it works:**
```
Main Thread (Non-blocking)
    ↓
    Offload to boundedElastic() pool
    ↓
Separate Thread (Blocking allowed)
    ↓ blocking operation happens here
    ↓
Return to Main Thread (Non-blocking)
```

**When to use:**
- Legacy JDBC code
- File I/O operations
- Third-party blocking libraries
- Network calls with RestTemplate

---

## ⚙️ Allowing Specific Blocking Calls

Sometimes you need to allow certain blocking operations that are **safe** (fast, in-memory, unavoidable):

### **Why Allow Certain Blocking?**

Not all "blocking" operations are problematic. Some are:
- ⚡ **Very fast** (microseconds) - don't hold thread long
- 💾 **CPU-bound** - in-memory, not I/O
- 🏭 **Standard practice** - industry accepted
- 🚫 **No alternative** - unavoidable in framework

---

### **Configuration Example:**

```java
@PostConstruct
public void init() {
    BlockHound.install(builder -> {
        // Allow Jackson JSON parsing
        builder.allowBlockingCallsInside(
            "com.fasterxml.jackson.databind.ObjectMapper",
            "readValue"
        );
        
        // Allow logging frameworks
        builder.allowBlockingCallsInside(
            "ch.qos.logback.classic.Logger",
            "callAppenders"
        );
        
        // Allow specific test code
        builder.allowBlockingCallsInside(
            "org.junit.jupiter.engine.execution.ExecutableInvoker",
            "invoke"
        );
    });
}
```

---

### **📖 Detailed Explanation:**

#### **1. Jackson JSON Parsing** ✅

**Why technically blocking?**
```java
// Jackson parsing is synchronous
ObjectMapper mapper = new ObjectMapper();
User user = mapper.readValue(json, User.class);  // Blocks until complete
```

**Why safe to allow?**
- ⚡ **Very fast**: Microseconds to parse JSON (in-memory operation)
- 💾 **CPU-bound**: No network or disk I/O involved
- 🏭 **Unavoidable**: Spring WebFlux uses Jackson internally
- ✅ **No impact**: Doesn't exhaust thread pool

**Real-world impact:**
```
Parsing 1KB JSON: ~0.1ms
Parsing 10KB JSON: ~1ms
Parsing 100KB JSON: ~10ms

Compare to:
Network call: 100-500ms
Database query: 50-200ms
File read: 10-100ms
```

**What happens if you DON'T allow:**
```
❌ Every HTTP request will throw BlockingOperationError
❌ .bodyToMono(UserDto.class) will fail
❌ Application won't work at all!
```

**Conclusion:** Jackson is a **"necessary evil"** - accepted trade-off in all reactive frameworks.

---

#### **2. Logging (Logback/SLF4J)** ✅

**Why technically blocking?**
```java
log.info("User loaded: {}", userId);  // May write to file/console
```

**Why safe to allow?**
- ⚡ **Async appenders available**: Logback has AsyncAppender
- 🎯 **Essential for debugging**: Can't run without logs
- ✅ **Buffered I/O**: Most logging frameworks buffer writes
- 🔧 **Configurable**: Can use async file appenders

**Best practice configuration:**
```xml
<!-- logback.xml -->
<appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="FILE" />
    <queueSize>512</queueSize>
    <discardingThreshold>0</discardingThreshold>
</appender>
```

**Conclusion:** Logging is **essential for operations** - optimize with async appenders.

---

#### **3. Test Frameworks (JUnit)** ✅

**Why safe to allow?**
- 🧪 **Test environment only**: Not in production
- ✅ **Test infrastructure**: Reflection, setup, etc
- 🎯 **Necessary**: Tests won't run otherwise

**Conclusion:** Allow in test config to enable testing reactive code.

---

### **❌ What You Should NEVER Allow:**

Even if it "fixes" BlockHound errors, **DON'T allow these:**

#### **1. JDBC Database Calls** ❌

```java
// ❌ BAD - Never do this!
builder.allowBlockingCallsInside("java.sql.Connection", "prepareStatement");
builder.allowBlockingCallsInside("java.sql.Statement", "executeQuery");
```

**Why this is WRONG:**
- 🐌 **Slow**: Database queries take 50-200ms
- 🔥 **Thread exhaustion**: Blocks event loop threads
- ❌ **Defeats reactive purpose**: No scalability benefit
- ✅ **Alternative exists**: Use R2DBC instead!

**Correct approach:**
```java
// ✅ Use R2DBC (reactive database driver)
return r2dbcTemplate.queryForObject(
    "SELECT * FROM users WHERE id = ?",
    User.class,
    id
);
```

---

#### **2. Thread.sleep()** ❌

```java
// ❌ BAD - Never do this!
builder.allowBlockingCallsInside("java.lang.Thread", "sleep");
```

**Why this is WRONG:**
- 🐌 **Real blocking**: Holds thread for entire duration
- 🔥 **Thread pool exhaustion**: Can't handle concurrent requests
- ✅ **Alternative exists**: Use `Mono.delay()` instead!

**Correct approach:**
```java
// ✅ Non-blocking delay
return Mono.delay(Duration.ofSeconds(1))
    .map(tick -> "result");
```

---

#### **3. File I/O** ❌

```java
// ❌ BAD - Never do this!
builder.allowBlockingCallsInside("java.io.FileInputStream", "read");
builder.allowBlockingCallsInside("java.nio.file.Files", "readString");
```

**Why this is WRONG:**
- 🐌 **I/O blocking**: Disk reads are slow
- ❌ **Scalability issue**: Can't handle many concurrent file operations
- ✅ **Alternative exists**: Use `DataBufferUtils` or async file APIs

---

#### **4. RestTemplate** ❌

```java
// ❌ BAD - Never do this!
builder.allowBlockingCallsInside("org.springframework.web.client.RestTemplate");
```

**Why this is WRONG:**
- 🐌 **Network I/O**: HTTP calls are slow (100-500ms)
- 🔥 **Major bottleneck**: Defeats entire purpose of reactive
- ✅ **Alternative exists**: Use `WebClient` instead!

---

### **🎯 Golden Rules for Allowing Blocking:**

| Rule | Example | Allow? |
|------|---------|--------|
| **Fast (<1ms) in-memory operations** | Jackson JSON parsing | ✅ YES |
| **Essential infrastructure** | Logging (with async) | ✅ YES |
| **Slow I/O operations** | JDBC, File I/O, Network | ❌ NO |
| **Thread sleeping/waiting** | Thread.sleep(), .wait() | ❌ NO |
| **Has reactive alternative** | RestTemplate, JDBC | ❌ NO |
| **No realistic alternative** | JSON parsing, Logging | ✅ YES |

---

### **🧪 How to Decide:**

**Ask yourself:**
1. **Is it fast (<1ms)?** → Maybe allow
2. **Is it in-memory (CPU-bound)?** → Maybe allow
3. **Does it do I/O (network/disk)?** → **DON'T allow**
4. **Is there a reactive alternative?** → **Use alternative, don't allow**
5. **Is it essential infrastructure?** → Maybe allow

**When in doubt:** DON'T allow it. Find the reactive alternative instead!

---

### **✅ Summary:**

**Safe to Allow (Accepted Trade-offs):**
- ✅ Jackson JSON parsing (fast, unavoidable)
- ✅ Logging with async appenders (essential)
- ✅ Test framework internals (test only)

**NEVER Allow (Real Problems):**
- ❌ JDBC database calls → Use R2DBC
- ❌ Thread.sleep() → Use Mono.delay()
- ❌ File I/O → Use DataBufferUtils
- ❌ RestTemplate → Use WebClient
- ❌ Synchronized blocks → Use AtomicReference

**Remember:** The goal is to catch **real blocking problems**, not to eliminate every microsecond delay. Focus on operations that hold threads for **milliseconds or longer**.

---

## 🧪 Testing with BlockHound

### **Test Example:**
```java
@SpringBootTest
class PostServiceTest {
    
    @Autowired
    private PostService postService;
    
    @Test
    void getPost_shouldBeNonBlocking() {
        // BlockHound will catch any blocking operations
        StepVerifier.create(postService.getPost(1L))
            .expectNextMatches(post -> post.getId().equals(1L))
            .verifyComplete();
        
        // If blocking detected → Test fails with BlockingOperationError!
    }
    
    @Test
    void getAllPosts_shouldBeNonBlocking() {
        StepVerifier.create(postService.getAllPosts())
            .expectNextCount(100)
            .verifyComplete();
        // ✅ Proves no blocking operations in entire chain
    }
}
```

---

## 🌍 Environment Configuration

### **DEV/TEST: Enable BlockHound**
```properties
# application-dev.properties
spring.profiles.active=dev
# BlockHound will be enabled
```

### **PROD: Disable BlockHound**
```properties
# application-prod.properties
spring.profiles.active=prod
# BlockHound will be disabled (performance)
```

### **Why Disable in Production?**
- ⚠️ Performance overhead (~5-10%)
- ⚠️ Not necessary once code is validated
- ✅ Use in CI/CD pipeline instead

---

## ✅ Best Practices

1. **Enable in DEV/TEST only**
   - ✅ Catch violations early
   - ❌ Don't run in PROD

2. **Fix violations immediately**
   - Don't suppress with `allowBlockingCallsInside()` unless necessary
   - Refactor to non-blocking alternative

3. **Run tests with BlockHound**
   - Every test becomes a blocking detector
   - CI/CD pipeline catches violations

4. **Document allowed blocking**
   - If you must allow blocking, document why
   - Review regularly

5. **Use proper schedulers**
   - `Schedulers.boundedElastic()` for unavoidable blocking
   - `Schedulers.parallel()` for CPU-intensive
   - Default scheduler for I/O

---

## 📊 BlockHound vs Manual Testing

| Approach | Detection Time | Effort | Coverage |
|----------|---------------|--------|----------|
| Manual review | ❌ Never | High | Low |
| Integration testing | ⚠️ Late | High | Medium |
| **BlockHound** | ✅ Immediate | Low | **100%** |

---

## 🎯 Summary

### **Key Points:**
- BlockHound = automatic detector for blocking calls
- Throws `BlockingOperationError` immediately
- Enable in DEV/TEST, disable in PROD
- Use `Schedulers.boundedElastic()` for unavoidable blocking
- Every test becomes a blocking detector

### **Golden Rules:**
1. ✅ No `Thread.sleep()` → Use `Mono.delay()`
2. ✅ No `JDBC` → Use `R2DBC`
3. ✅ No `RestTemplate` → Use `WebClient`
4. ✅ No `synchronized` → Use `AtomicReference`
5. ✅ No blocking I/O → Use reactive alternatives

---

## 🔄 Alternatives to BlockHound

Kalau **TIDAK mau pakai BlockHound**, ada beberapa alternatif:

### **Option 1: Manual Code Review** ⚠️

**Pros:**
- ✅ No dependencies
- ✅ No performance overhead
- ✅ Full control

**Cons:**
- ❌ Human error prone
- ❌ Time consuming
- ❌ Can't catch all cases

**Checklist untuk review:**
```java
// ❌ Look for these patterns:
- Thread.sleep()
- .wait() / .join()
- synchronized blocks
- JDBC calls (Connection, Statement)
- RestTemplate
- FileInputStream/FileOutputStream
- BufferedReader.readLine()
- Files.readString() / Files.write()
```

---

### **Option 2: Performance Monitoring** 📊

**Tools:**
- **Spring Boot Actuator** - Monitor response times
- **Micrometer** - Metrics collection
- **Application Performance Monitoring (APM)** - NewRelic, Datadog, Dynatrace

**How to detect:**
```properties
# application.properties
management.endpoints.web.exposure.include=metrics,health

# Monitor these metrics:
# - http.server.requests (high latency = possible blocking)
# - jvm.threads.live (increasing = thread pool exhaustion)
# - reactor.netty.eventloop.pending.tasks (backlog = blocking)
```

**Pros:**
- ✅ Works in production
- ✅ Historical data
- ✅ Real user impact

**Cons:**
- ❌ Detects AFTER problem occurs
- ❌ Harder to pinpoint exact code
- ❌ Requires setup & monitoring

---

### **Option 3: Integration Testing with Load** 🚀

**Strategy:** Run load tests and check thread pool usage

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class LoadTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void shouldHandleHighConcurrency() {
        // Simulate 1000 concurrent requests
        List<Mono<ResponseEntity<Post>>> requests = IntStream.range(0, 1000)
            .mapToObj(i -> webTestClient.get()
                .uri("/api/posts/1")
                .exchange()
                .returnResult(Post.class)
                .getResponseEntity())
            .collect(Collectors.toList());
        
        // If blocking, this will timeout or fail
        Flux.merge(requests)
            .collectList()
            .block(Duration.ofSeconds(5));  // Should complete fast if non-blocking
    }
}
```

**Pros:**
- ✅ Realistic scenario
- ✅ Catches performance issues
- ✅ No special dependencies

**Cons:**
- ❌ Slow to run
- ❌ Hard to pinpoint exact issue
- ❌ May miss subtle blocking

---

### **Option 4: Reactor Debug Agent** 🔍

**Alternative tool** dari Reactor team (lighter than BlockHound):

```xml
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-tools</artifactId>
</dependency>
```

```java
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        // Enable reactor debugging (better stack traces)
        ReactorDebugAgent.init();
        SpringApplication.run(App.class, args);
    }
}
```

**What it provides:**
- ✅ Better stack traces in reactive code
- ✅ Checkpoint tracking
- ⚠️ Doesn't detect blocking (but helps debug)

**Pros:**
- ✅ Lighter than BlockHound
- ✅ Better error messages
- ✅ Can run in production (with flag)

**Cons:**
- ❌ Doesn't detect blocking calls
- ❌ Only helps with debugging

---

### **Option 5: Thread Pool Monitoring** 📈

**Check Netty event loop usage:**

```java
@Component
public class ThreadPoolMonitor {
    
    @Scheduled(fixedRate = 5000)
    public void monitorEventLoops() {
        // Get Netty event loop metrics
        EventLoopGroup eventLoopGroup = ...; // From Netty config
        
        if (eventLoopGroup instanceof NioEventLoopGroup) {
            NioEventLoopGroup nioGroup = (NioEventLoopGroup) eventLoopGroup;
            // Check if threads are blocked
            // High blocked threads = possible blocking operations
        }
    }
}
```

**Indicator of blocking:**
- Event loop threads keep increasing
- High CPU usage on event loop threads
- Request latency spikes

**Pros:**
- ✅ Production-safe
- ✅ Real-time monitoring

**Cons:**
- ❌ Indirect detection
- ❌ Requires deep understanding

---

## 📊 Comparison Table

| Approach | Detection Speed | Accuracy | Effort | Production Safe |
|----------|----------------|----------|--------|-----------------|
| **BlockHound** | ⚡ Instant | 🎯 100% | ✅ Low | ⚠️ Dev only |
| Manual Review | 🐌 Slow | ⚠️ 60% | ❌ High | ✅ Yes |
| Performance Monitoring | 🕐 Delayed | ⚠️ 70% | 🟡 Medium | ✅ Yes |
| Load Testing | 🐌 Slow | ⚠️ 80% | ❌ High | 🟡 CI/CD |
| Reactor Debug | 🕐 N/A | ❌ 0% | ✅ Low | ✅ Yes |
| Thread Monitoring | 🕐 Delayed | ⚠️ 50% | 🟡 Medium | ✅ Yes |

---

## 🎯 Recommendation

### **For Development:**
✅ **Use BlockHound** - Best detection, catches issues immediately

### **For Production:**
✅ **Performance Monitoring** + **Metrics** - Safe, provides insights

### **For CI/CD:**
✅ **BlockHound in tests** + **Load testing** - Automated validation

### **If You Can't Use BlockHound:**
1. **Manual code review** (use checklist above)
2. **Load testing** in CI/CD
3. **Metrics monitoring** in production
4. **Regular performance audits**

---

## 🏁 Conclusion

**BlockHound is the BEST tool** for detecting blocking calls, but alternatives exist if you can't use it:

- 🥇 **Best:** BlockHound (dev/test)
- 🥈 **Good:** Performance Monitoring + Load Tests
- 🥉 **Fallback:** Manual Review + Metrics

**Bottom line:** BlockHound saves time and catches issues instantly. If you're serious about reactive code, use it in development! 🎯

---

## 📚 Related Guides

- **[REACTIVE_GUIDE.md](REACTIVE_GUIDE.md)** - Reactive programming patterns
- **[PURE_HEXAGONAL_ARCHITECTURE.md](PURE_HEXAGONAL_ARCHITECTURE.md)** - Where reactive fits
- **[TDD_HEXAGONAL_WORKFLOW.md](TDD_HEXAGONAL_WORKFLOW.md)** - Testing reactive code

---

**🔍 BlockHound: Your reactive code guardian!**

*Run it in development, sleep well in production.* 😴✨

