# Infrastructure Adapters Guide

## 📂 Folder Structure

```
infrastructure/
├── client/          ← External API calls (HTTP, gRPC, SOAP)
├── persistence/     ← Database (JPA, JDBC, MongoDB, etc)
├── cache/          ← Cache systems (Redis, Memcached)
├── messaging/      ← Message queues (Kafka, RabbitMQ, SQS)
├── storage/        ← File system, S3, cloud storage
└── web/            ← Inbound HTTP (REST Controllers)
```

---

## 🎯 **Adapter Types**

### 1. **Client Adapter** (External API)
**Location:** `infrastructure/client/`  
**Purpose:** Call external REST APIs, microservices  
**Example:** `PostApiClient.java` (JSONPlaceholder)

**When to use:**
- Calling third-party APIs
- Inter-service communication
- External payment gateways

---

### 2. **Persistence Adapter** (Database)
**Location:** `infrastructure/persistence/`  
**Purpose:** Database operations using JPA, JDBC, etc  
**Example:** `PostJpaAdapter.java`

**When to use:**
- Storing data to SQL databases (PostgreSQL, MySQL)
- NoSQL databases (MongoDB, Cassandra)
- Need transactions, complex queries

**Dependencies needed:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

---

### 3. **Cache Adapter** (Redis, Memcached)
**Location:** `infrastructure/cache/`  
**Purpose:** Caching for performance  
**Example:** `PostRedisAdapter.java`

**When to use:**
- Reduce database load
- Fast data retrieval
- Session management

**Dependencies needed:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

---

### 4. **Messaging Adapter** (Kafka, RabbitMQ)
**Location:** `infrastructure/messaging/`  
**Purpose:** Async messaging, event-driven architecture  
**Example:** `PostKafkaAdapter.java`

**When to use:**
- Event-driven microservices
- Async processing
- Message queues

**Dependencies needed:**
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

---

### 5. **Storage Adapter** (File System, S3)
**Location:** `infrastructure/storage/`  
**Purpose:** File operations, cloud storage  
**Example:** `PostFileAdapter.java`

**When to use:**
- Upload/download files
- Export to CSV/JSON
- S3, GCS, Azure Blob

---

## 🔥 **Usage Patterns**

### Single Adapter (Simple)
```java
@Service
public class PostService {
    private final PostRepository repository;  // Only one implementation
}
```

### Multiple Adapters with @Primary
```java
@Component
@Primary  // ← Default
public class PostJpaAdapter implements PostRepository { }

@Component
@Qualifier("api")
public class PostApiClient implements PostRepository { }
```

### Composite Adapter (Cache + DB)
```java
@Component
@Primary
public class PostCachedAdapter implements PostRepository {
    private final PostRedisAdapter cache;
    private final PostJpaAdapter database;
    
    public Mono<List<Post>> findAll() {
        return cache.findAll()
            .switchIfEmpty(
                database.findAll()
                    .doOnNext(cache::save)
            );
    }
}
```

---

## ✅ **Key Principles**

1. **All adapters implement domain interface** (port)
2. **Domain layer never knows about adapters**
3. **Easy to swap/mock adapters**
4. **One adapter = one responsibility**

---

## 📝 **Next Steps**

To activate an adapter:
1. Uncomment the `@Component` annotation
2. Add required dependencies to `pom.xml`
3. Configure connection in `application.properties`
4. Implement the methods
5. Spring will auto-inject via interface

Example:
```properties
# application.properties for JPA
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=user
spring.datasource.password=pass
spring.jpa.hibernate.ddl-auto=update
```

