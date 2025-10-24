package com.loginservice.app.infrastructure.composite;

import com.loginservice.app.domain.entity.Post;
import com.loginservice.app.domain.repository.PostRepository;
import reactor.core.publisher.Mono;
import java.util.List;

/**
 * Composite Adapter: Redis → Database
 * 
 * Simple flow:
 * 1. Cek Redis (cache) dulu
 * 2. Kalau ga ada (MISS) → ambil dari Database
 * 3. Simpan ke Redis untuk next request
 * 4. Return data
 * 
 * NOTE: Semua method dalam bentuk COMMENT dulu
 * Tinggal uncomment & implement sesuai kebutuhan
 */
// @Component  // Uncomment kalau mau aktifkan
// @Primary    // Ini yang akan dipake default
public class PostCachedDbAdapter implements PostRepository {

    // ========================================
    // STEP 1: Declare dependencies
    // ========================================
    
    // Redis adapter untuk cache
    // private final RedisTemplate<String, Post> redisTemplate;
    
    // Database adapter (bisa JPA, JDBC, dll)
    // private final PostJpaRepository jpaRepository;
    
    // Constructor injection
    // public PostCachedDbAdapter(
    //     RedisTemplate<String, Post> redisTemplate,
    //     PostJpaRepository jpaRepository
    // ) {
    //     this.redisTemplate = redisTemplate;
    //     this.jpaRepository = jpaRepository;
    // }

    // ========================================
    // STEP 2: Implement findById dengan Cache
    // ========================================
    
    @Override
    public Mono<Post> findById(Long id) {
        
        // FLOW:
        // 1. Check Redis cache first
        // 2. If HIT → return immediately (FAST!)
        // 3. If MISS → get from database
        // 4. Save to Redis cache
        // 5. Return data
        
        // String cacheKey = "post:" + id;
        
        // return Mono.fromCallable(() -> {
        //     // STEP 1: Try to get from Redis
        //     Post cachedPost = redisTemplate.opsForValue().get(cacheKey);
        //     return cachedPost;
        // })
        // .flatMap(cachedPost -> {
        //     if (cachedPost != null) {
        //         // CACHE HIT! Return immediately
        //         System.out.println("✅ Cache HIT for post: " + id);
        //         return Mono.just(cachedPost);
        //     } else {
        //         // CACHE MISS! Get from database
        //         System.out.println("❌ Cache MISS for post: " + id);
        //         
        //         return Mono.fromCallable(() -> {
        //             // STEP 2: Get from database
        //             Optional<PostEntity> entityOpt = jpaRepository.findById(id);
        //             
        //             if (entityOpt.isEmpty()) {
        //                 throw new RuntimeException("Post not found: " + id);
        //             }
        //             
        //             // Convert entity → domain
        //             PostEntity entity = entityOpt.get();
        //             Post post = new Post(
        //                 entity.getUserId(),
        //                 entity.getId(),
        //                 entity.getTitle(),
        //                 entity.getBody()
        //             );
        //             
        //             // STEP 3: Save to Redis cache (TTL 10 minutes)
        //             redisTemplate.opsForValue().set(
        //                 cacheKey, 
        //                 post, 
        //                 Duration.ofMinutes(10)
        //             );
        //             System.out.println("💾 Cached to Redis: " + id);
        //             
        //             // STEP 4: Return post
        //             return post;
        //         });
        //     }
        // });
        
        throw new UnsupportedOperationException("Not implemented yet - uncomment code above");
    }

    // ========================================
    // STEP 3: Implement findAll
    // ========================================
    
    @Override
    public Mono<List<Post>> findAll() {
        
        // FLOW (similar to findById):
        // 1. Check Redis for "posts:all" key
        // 2. If HIT → return cached list
        // 3. If MISS → get all from database
        // 4. Cache the list
        // 5. Return
        
        // String cacheKey = "posts:all";
        
        // return Mono.fromCallable(() -> {
        //     // Try cache first
        //     List<Post> cached = redisTemplate.opsForList()
        //         .range(cacheKey, 0, -1);
        //     
        //     if (cached != null && !cached.isEmpty()) {
        //         System.out.println("✅ Cache HIT for all posts");
        //         return cached;
        //     } else {
        //         System.out.println("❌ Cache MISS for all posts");
        //         
        //         // Get from database
        //         List<PostEntity> entities = jpaRepository.findAll();
        //         
        //         // Convert to domain
        //         List<Post> posts = entities.stream()
        //             .map(e -> new Post(
        //                 e.getUserId(),
        //                 e.getId(),
        //                 e.getTitle(),
        //                 e.getBody()
        //             ))
        //             .toList();
        //         
        //         // Cache them
        //         posts.forEach(post -> 
        //             redisTemplate.opsForList().rightPush(cacheKey, post)
        //         );
        //         redisTemplate.expire(cacheKey, Duration.ofMinutes(10));
        //         
        //         System.out.println("💾 Cached " + posts.size() + " posts");
        //         
        //         return posts;
        //     }
        // });
        
        throw new UnsupportedOperationException("Not implemented yet - uncomment code above");
    }

    // ========================================
    // OPTIONAL: Helper methods
    // ========================================
    
    /**
     * Invalidate cache when data changes
     * Call this after save/update/delete operations
     */
    // public void invalidateCache(Long postId) {
    //     String cacheKey = "post:" + postId;
    //     redisTemplate.delete(cacheKey);
    //     redisTemplate.delete("posts:all"); // Also clear list cache
    //     System.out.println("🗑️ Invalidated cache for post: " + postId);
    // }
    
    /**
     * Save post to database AND invalidate cache
     */
    // public Mono<Post> save(Post post) {
    //     return Mono.fromCallable(() -> {
    //         // Convert domain → entity
    //         PostEntity entity = new PostEntity();
    //         entity.setUserId(post.userId());
    //         entity.setTitle(post.title());
    //         entity.setBody(post.body());
    //         
    //         // Save to database
    //         PostEntity saved = jpaRepository.save(entity);
    //         
    //         // Invalidate cache
    //         invalidateCache(saved.getId());
    //         
    //         // Return domain
    //         return new Post(
    //             saved.getUserId(),
    //             saved.getId(),
    //             saved.getTitle(),
    //             saved.getBody()
    //         );
    //     });
    // }
}

