# 🎯 TDD GUIDE - Post Feature

## Netflix Hexagonal Architecture Structure
```
domain/
  ├── entity/Post.java              (domain model)
  └── repository/PostRepository.java (interface)
  
application/
  └── interactor/GetPostInteractor.java (use case)
  
infrastructure/
  ├── web/PostController.java
  └── client/post/PostApiClient.java (implements repository)
```

---

## 🔴 STEP 1: RED - Write Failing Test

**File**: `src/test/java/com/loginservice/app/domain/entity/PostTest.java`

```java
package com.loginservice.app.domain.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PostTest {
    
    @Test
    void shouldBeValid_whenHasRequiredFields() {
        // Given
        Post post = new Post(1L, 1L, "Valid Title", "Body");
        
        // When
        boolean result = post.isValid();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void shouldBeInvalid_whenTitleTooShort() {
        // Given
        Post post = new Post(1L, 1L, "ab", "Body");
        
        // When
        boolean result = post.isValid();
        
        // Then
        assertFalse(result);
    }
}
```

**Run test**: `mvn test`
❌ **EXPECTED**: Test gagal karena class `Post` belum ada

---

## 🟢 STEP 2: GREEN - Make It Pass

**File**: `src/main/java/com/loginservice/app/domain/entity/Post.java`

```java
package com.loginservice.app.domain.entity;

public class Post {
    private final Long userId;
    private final Long id;
    private final String title;
    private final String body;
    
    public Post(Long userId, Long id, String title, String body) {
        this.userId = userId;
        this.id = id;
        this.title = title;
        this.body = body;
    }
    
    public boolean isValid() {
        return id != null && 
               title != null && 
               !title.trim().isEmpty() &&
               title.length() >= 3;
    }
    
    // Getters
    public Long getUserId() { return userId; }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
}
```

**Run test**: `mvn test -Dtest=PostTest`
✅ **EXPECTED**: Test pass!

---

## 🔵 STEP 3: REFACTOR - Add More Tests & Business Logic

**Add more tests** untuk business rules:

```java
@Test
void shouldBelongToUser_whenUserIdMatches() {
    Post post = new Post(5L, 1L, "Title", "Body");
    assertTrue(post.belongsToUser(5L));
}

@Test
void shouldNotBelongToUser_whenUserIdDoesNotMatch() {
    Post post = new Post(5L, 1L, "Title", "Body");
    assertFalse(post.belongsToUser(10L));
}
```

**Implement** method di `Post.java`:

```java
public boolean belongsToUser(Long userId) {
    return this.userId != null && this.userId.equals(userId);
}
```

---

## 🎯 NEXT STEPS - Interactor Test

**File**: `src/test/java/com/loginservice/app/application/interactor/GetPostInteractorTest.java`

```java
@ExtendWith(MockitoExtension.class)
class GetPostInteractorTest {
    
    @Mock
    private PostRepository postRepository;
    
    @InjectMocks
    private GetPostInteractor interactor;
    
    @Test
    void shouldReturnPost_whenValidPostExists() {
        // Given
        Post post = new Post(1L, 1L, "Valid Title", "Body");
        when(postRepository.findById(1L)).thenReturn(Mono.just(post));
        
        // When
        Mono<Post> result = interactor.execute(1L);
        
        // Then
        StepVerifier.create(result)
            .expectNext(post)
            .verifyComplete();
    }
}
```

❌ Run → Fail (PostRepository & Interactor belum ada)

✅ Implement → Pass

---

## 📝 TDD RULES

1. **Never write code** without failing test first
2. **Write smallest** possible code to pass
3. **Refactor** when all tests green
4. **One test** at a time
5. **Test behavior**, not implementation

---

## 🚀 START HERE

```bash
# 1. Create test file first
touch src/test/java/com/loginservice/app/domain/entity/PostTest.java

# 2. Write failing test
# 3. Run: mvn test -Dtest=PostTest
# 4. Write minimal code to pass
# 5. Repeat!
```

Good luck! 🔥

