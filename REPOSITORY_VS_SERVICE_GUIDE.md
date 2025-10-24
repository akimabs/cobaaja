# Repository vs Service: Kapan Taro Method Dimana?

## 🎯 **Golden Rule**

```
Repository  = "HOW to get/save data"     (Technical)
Service     = "WHAT to do with data"     (Business)
```

---

## ✅ **Repository Methods** (Data Access Only)

### Basic CRUD
```java
public interface PostRepository {
    Mono<Post> findById(Long id);
    Mono<List<Post>> findAll();
    Mono<Post> save(Post post);
    Mono<Void> deleteById(Long id);
}
```

### Query Methods
```java
public interface PostRepository {
    Flux<Post> findByUserId(Long userId);
    Flux<Post> findByTitleContaining(String keyword);
    Flux<Post> findByCreatedAtBefore(LocalDateTime date);
    Mono<Long> countByUserId(Long userId);
}
```

**Ciri-ciri:**
- ✅ Prefix: `find`, `save`, `delete`, `count`, `exists`
- ✅ Simple data retrieval/storage
- ✅ No business logic
- ✅ Can be directly mapped to DB query

---

## ✅ **Service Methods** (Business Logic)

### Business Rules
```java
@Service
public class PostService {
    
    // ❌ JANGAN di Repository
    // ✅ DI Service - ada validation/business rule
    public Mono<Post> getValidPost(Long id) {
        return repository.findById(id)
            .filter(Post::isValid)
            .switchIfEmpty(Mono.error(new InvalidPostException()));
    }
    
    // ❌ JANGAN di Repository
    // ✅ DI Service - complex logic
    public Mono<Post> createPost(CreatePostRequest request) {
        // 1. Validate user exists
        return userService.validateUser(request.getUserId())
            // 2. Check quota
            .flatMap(user -> checkPostQuota(user))
            // 3. Create post
            .map(user -> new Post(
                request.getUserId(),
                generateId(),
                sanitizeTitle(request.getTitle()),  // Business logic
                request.getBody()
            ))
            // 4. Save
            .flatMap(repository::save)
            // 5. Send notification
            .doOnSuccess(this::sendNotification);
    }
}
```

### Orchestration
```java
@Service
public class PostService {
    
    // ❌ JANGAN di Repository
    // ✅ DI Service - orchestrate multiple services
    public Mono<PostWithComments> getPostDetails(Long id) {
        Mono<Post> postMono = repository.findById(id);
        Mono<User> userMono = userService.getUser(id);
        Mono<List<Comment>> commentsMono = commentService.getComments(id);
        
        return Mono.zip(postMono, userMono, commentsMono)
            .map(tuple -> new PostWithComments(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3()
            ));
    }
}
```

---

## 📊 **Decision Tree**

```
Question: "Should this method be in Repository or Service?"

┌─────────────────────────────────────┐
│ Does it involve business logic?     │
│ (validation, calculation, rules)    │
└─────────┬───────────────────────────┘
          │
    YES ──┤
          │
          └──> Put in SERVICE
          
          │
    NO  ──┤
          │
          ▼
┌─────────────────────────────────────┐
│ Is it ONLY data access?             │
│ (find, save, delete from storage)   │
└─────────┬───────────────────────────┘
          │
    YES ──┤
          │
          └──> Put in REPOSITORY
          
          │
    NO  ──┤
          │
          └──> Probably SERVICE
```

---

## 🔴 **Common Mistakes**

### ❌ WRONG: Business Logic in Repository
```java
public interface PostRepository {
    // ❌ Business logic leak!
    Mono<Post> findValidPostById(Long id);  // What is "valid"? Business rule!
    Mono<Post> findPublishedPostById(Long id);  // "published" = business state
    Mono<List<Post>> findRecentPosts();  // "recent" = business definition
}
```

**Why wrong?**
- "Valid" is a business concept, not data access
- Repository shouldn't know business rules
- Hard to change business rules later

### ✅ CORRECT: Keep Repository Simple
```java
// Repository - simple data access
public interface PostRepository {
    Mono<Post> findById(Long id);
    Flux<Post> findAll();
    Flux<Post> findByCreatedAtAfter(LocalDateTime date);
}

// Service - business logic
@Service
public class PostService {
    public Mono<Post> getValidPost(Long id) {
        return repository.findById(id)
            .filter(Post::isValid);  // Business rule here
    }
    
    public Flux<Post> getRecentPosts() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        return repository.findByCreatedAtAfter(oneWeekAgo)  // Data access
            .filter(Post::isPublished);  // Business filter
    }
}
```

---

## 🎓 **Real World Examples**

### Example 1: E-commerce Order

```java
// ✅ Repository - data access
public interface OrderRepository {
    Mono<Order> findById(Long id);
    Flux<Order> findByUserId(Long userId);
    Flux<Order> findByStatus(OrderStatus status);
    Mono<Order> save(Order order);
}

// ✅ Service - business logic
@Service
public class OrderService {
    
    public Mono<Order> placeOrder(PlaceOrderRequest request) {
        // Business logic:
        // 1. Validate cart
        // 2. Check inventory
        // 3. Calculate total
        // 4. Apply discounts
        // 5. Create order
        // 6. Reserve inventory
        // 7. Send email
        
        return cartService.validateCart(request.getCartId())
            .flatMap(cart -> inventoryService.checkAvailability(cart))
            .flatMap(cart -> {
                BigDecimal total = calculateTotal(cart);  // Business logic
                BigDecimal discount = applyDiscounts(cart, total);  // Business logic
                
                Order order = new Order(
                    request.getUserId(),
                    cart,
                    total.subtract(discount)
                );
                
                return repository.save(order);
            })
            .flatMap(order -> 
                inventoryService.reserve(order)
                    .thenReturn(order)
            )
            .doOnSuccess(emailService::sendOrderConfirmation);
    }
}
```

### Example 2: Social Media Post

```java
// ✅ Repository - simple queries
public interface PostRepository {
    Mono<Post> findById(Long id);
    Flux<Post> findByUserId(Long userId);
    Flux<Post> findByHashtag(String hashtag);
    Mono<Post> save(Post post);
}

// ✅ Service - complex business logic
@Service
public class PostService {
    
    public Mono<Post> publishPost(Long postId) {
        return repository.findById(postId)
            // Business rules
            .filter(Post::isValid)
            .filter(post -> !post.containsProfanity())
            .filter(post -> post.getContent().length() <= 280)
            // State change
            .map(post -> {
                post.setStatus(PostStatus.PUBLISHED);
                post.setPublishedAt(LocalDateTime.now());
                return post;
            })
            // Save
            .flatMap(repository::save)
            // Side effects
            .doOnSuccess(post -> {
                notificationService.notifyFollowers(post);
                analyticsService.trackPublish(post);
                searchService.indexPost(post);
            });
    }
    
    public Flux<Post> getFeedForUser(Long userId) {
        // Business logic: Get following users, aggregate posts, rank
        return userService.getFollowing(userId)
            .flatMapMany(following -> 
                repository.findByUserId(following.getId())
            )
            .filter(Post::isPublished)
            .sort(this::rankByRelevance)  // Business logic
            .take(50);
    }
}
```

---

## 📏 **Guidelines**

### Repository Should:
- ✅ Be thin/simple
- ✅ Only data access operations
- ✅ No business logic
- ✅ Easy to understand
- ✅ Methods like: `find*`, `save*`, `delete*`, `count*`

### Service Should:
- ✅ Contain business logic
- ✅ Orchestrate multiple repositories
- ✅ Validation & rules
- ✅ Complex transformations
- ✅ Side effects (events, notifications)

---

## 🎯 **Summary Table**

| Concern | Repository | Service |
|---------|-----------|---------|
| **Data access** | ✅ YES | ❌ Via repository |
| **Business rules** | ❌ NO | ✅ YES |
| **Validation** | ❌ NO | ✅ YES |
| **Orchestration** | ❌ NO | ✅ YES |
| **Side effects** | ❌ NO | ✅ YES |
| **State changes** | ❌ NO | ✅ YES |
| **Calculations** | ❌ NO | ✅ YES |

---

## 💡 **Key Takeaway**

> **Repository = Data Access Layer (dumb)**
> 
> **Service = Business Logic Layer (smart)**

**Keep Repository skinny, keep Service rich!**

