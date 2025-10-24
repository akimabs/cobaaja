# Simple Flow Example: Request → JSON Response

## 🚀 **Real Flow Execution**

### Request:
```bash
curl http://localhost:8080/api/posts/1
```

---

## 🔄 **Step by Step:**

### **Step 1: HTTP Request masuk ke Controller**
```
GET /api/posts/1
    ↓
PostController.getPost(1)
```

**Code:**
```java
@GetMapping("/{id}")
public Mono<PostResponse> getPost(@PathVariable Long id) {
    return postService.getPost(id)       // ← Call service
        .map(PostResponse::from);        // ← Convert to JSON
}
```

---

### **Step 2: Controller panggil Service**
```
PostController.getPost(1)
    ↓
PostService.getPost(1)
```

**Code:**
```java
public Mono<Post> getPost(Long id) {
    return postRepository.findById(id)   // ← Call repository (interface!)
        .filter(Post::isValid);
}
```

**Note:** Service cuma tahu `PostRepository` (interface), ga tahu adapter apa yang dipake!

---

### **Step 3: Spring DI inject adapter**
```
PostService calls: postRepository.findById(1)
    ↓ (Spring DI)
PostApiClient.findById(1)  ← Currently active (no composite yet)
```

**Spring pilih:** `PostApiClient` karena ada `@Component` dan implement `PostRepository`

---

### **Step 4: Adapter fetch data**
```
PostApiClient.findById(1)
    ↓
WebClient GET https://jsonplaceholder.typicode.com/posts/1
    ↓
Receive JSON from API
    ↓
Convert PostDto → Post (domain entity)
    ↓
Return Mono<Post>
```

**Code:**
```java
public Mono<Post> findById(Long id) {
    return webClient.get()
        .uri("/posts/{id}", id)         // API call
        .retrieve()
        .bodyToMono(PostDto.class)       // Parse JSON
        .map(dto -> new Post(            // Convert to domain
            dto.getUserId(),
            dto.getId(), 
            dto.getTitle(), 
            dto.getBody()
        ));
}
```

---

### **Step 5: Data balik ke Service**
```
PostApiClient returns: Mono<Post>
    ↓
PostService receives: Mono<Post>
    ↓
Apply business logic: .filter(Post::isValid)
    ↓
Return to Controller: Mono<Post>
```

---

### **Step 6: Service return ke Controller**
```
PostService returns: Mono<Post>
    ↓
PostController receives: Mono<Post>
```

**Code:**
```java
return postService.getPost(id)           // Receive Mono<Post>
    .map(PostResponse::from);            // Convert Post → PostResponse
```

---

### **Step 7: Controller convert domain → DTO**
```
Post (domain entity)
    ↓
PostResponse.from(post)
    ↓
PostResponse (DTO for JSON)
```

**Code:**
```java
public static PostResponse from(Post post) {
    return new PostResponse(
        post.userId(),
        post.id(),
        post.title(),
        post.body()
    );
}
```

---

### **Step 8: Spring WebFlux serialize to JSON**
```
PostResponse {
    userId = 1,
    id = 1,
    title = "sunt aut facere",
    body = "quia et suscipit..."
}
    ↓ (Spring auto-serialize)
JSON Response
```

---

### **Step 9: HTTP Response**
```json
{
  "userId": 1,
  "id": 1,
  "title": "sunt aut facere repellat provident occaecati",
  "body": "quia et suscipit\nsuscipit recusandae consequuntur..."
}
```

---

## 📊 **Visual Flow:**

```
┌──────────────────────────────────────────┐
│  Client                                  │
│  GET /api/posts/1                        │
└───────────────┬──────────────────────────┘
                ↓
┌───────────────────────────────────────────┐
│  PostController                           │
│  @GetMapping("/{id}")                     │
│  getPost(1)                               │
└───────────────┬───────────────────────────┘
                ↓
┌───────────────────────────────────────────┐
│  PostService                              │
│  getPost(1)                               │
│  → postRepository.findById(1)             │
└───────────────┬───────────────────────────┘
                ↓ (Spring DI inject)
┌───────────────────────────────────────────┐
│  PostApiClient                            │
│  implements PostRepository                │
│  → WebClient GET /posts/1                 │
└───────────────┬───────────────────────────┘
                ↓
┌───────────────────────────────────────────┐
│  JSONPlaceholder API                      │
│  https://jsonplaceholder.typicode.com     │
│  Returns JSON                             │
└───────────────┬───────────────────────────┘
                ↓
┌───────────────────────────────────────────┐
│  PostApiClient                            │
│  Convert PostDto → Post (domain)          │
│  Return Mono<Post>                        │
└───────────────┬───────────────────────────┘
                ↓
┌───────────────────────────────────────────┐
│  PostService                              │
│  Receive Mono<Post>                       │
│  Apply business logic                     │
│  Return Mono<Post>                        │
└───────────────┬───────────────────────────┘
                ↓
┌───────────────────────────────────────────┐
│  PostController                           │
│  Convert Post → PostResponse (DTO)        │
│  Return Mono<PostResponse>                │
└───────────────┬───────────────────────────┘
                ↓
┌───────────────────────────────────────────┐
│  Spring WebFlux                           │
│  Serialize PostResponse → JSON            │
└───────────────┬───────────────────────────┘
                ↓
┌───────────────────────────────────────────┐
│  Client receives JSON                     │
│  {                                         │
│    "userId": 1,                            │
│    "id": 1,                                │
│    "title": "sunt aut facere",             │
│    "body": "quia et suscipit..."           │
│  }                                         │
└────────────────────────────────────────────┘
```

---

## 🎯 **Key Points:**

1. ✅ **Controller** hanya tahu **Service**
2. ✅ **Service** hanya tahu **Repository interface**
3. ✅ **Spring DI** inject adapter automatically
4. ✅ **Domain entity** (Post) flow through layers
5. ✅ **DTO** (PostResponse) only at Controller (web boundary)
6. ✅ **JSON** auto-serialized by Spring WebFlux

---

## 🔧 **Kalo pake Composite Adapter:**

Kalau nanti kamu aktifin `PostCacheStorageAdapter` dengan `@Primary`:

```
PostService calls: postRepository.findById(1)
    ↓ (Spring DI inject @Primary)
PostCacheStorageAdapter.findById(1)  ← Composite adapter
    ↓
    ├─ Check Redis
    ├─ If MISS → Get from API
    ├─ Save to Minio
    ├─ Cache to Redis
    └─ Return Post
```

**Service TIDAK BERUBAH!** Still cuma tahu interface! 🪄

---

## ✅ **Summary:**

```
HTTP Request (JSON)
    ↓
PostController → extract id
    ↓
PostService → business logic
    ↓
PostRepository (interface)
    ↓
PostApiClient (or Composite) → fetch data
    ↓
Return Post (domain entity)
    ↓
PostController → convert to PostResponse
    ↓
Spring → serialize to JSON
    ↓
HTTP Response (JSON)
```

**Magic:** Separation of concerns di setiap layer! 🚀

