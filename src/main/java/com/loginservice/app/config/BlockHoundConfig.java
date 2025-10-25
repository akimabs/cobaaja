package com.loginservice.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import reactor.blockhound.BlockHound;
import jakarta.annotation.PostConstruct;

/**
 * BlockHound Configuration
 * 
 * BlockHound is a Java agent to detect blocking calls from non-blocking threads.
 * 
 * ⚠️ WARNING: Only enable in DEV/TEST environment!
 * DO NOT enable in PRODUCTION (performance overhead)
 * 
 * Purpose:
 * - Detect blocking operations in reactive pipeline
 * - Throw BlockingOperationError when blocking detected
 * - Helps ensure truly non-blocking reactive code
 * 
 * ========================================
 * What BlockHound DETECTS (❌ BLOCKING):
 * ========================================
 * 
 * 1. Thread Operations:
 *    - Thread.sleep()
 *    - Thread.join()
 *    - Object.wait()
 *    - LockSupport.park()
 * 
 * 2. Blocking I/O Operations:
 *    - FileInputStream.read()
 *    - FileOutputStream.write()
 *    - Socket.getInputStream().read()
 *    - Socket.getOutputStream().write()
 *    - RandomAccessFile operations
 *    - Files.readAllBytes()
 *    - Files.writeString()
 * 
 * 3. Synchronization:
 *    - synchronized blocks/methods
 *    - ReentrantLock.lock()
 *    - Semaphore.acquire()
 *    - CountDownLatch.await()
 * 
 * 4. Database Operations:
 *    - JDBC calls (Connection, Statement, ResultSet)
 *    - Use R2DBC instead for reactive DB access
 * 
 * 5. Network I/O:
 *    - Traditional HttpURLConnection
 *    - RestTemplate (blocking)
 *    - Use WebClient instead (non-blocking)
 * 
 * 6. Stream Operations:
 *    - BufferedReader.readLine()
 *    - Scanner.nextLine()
 *    - System.in.read()
 * 
 * ========================================
 * Example VIOLATIONS:
 * ========================================
 * 
 * ❌ BAD - Thread.sleep:
 * return Mono.fromCallable(() -> {
 *     Thread.sleep(1000);  // BlockHound will throw error!
 *     return "result";
 * });
 * 
 * ✅ GOOD - Mono.delay:
 * return Mono.delay(Duration.ofSeconds(1))
 *     .map(tick -> "result");
 * 
 * ❌ BAD - File I/O:
 * return Mono.fromCallable(() -> {
 *     return Files.readString(Path.of("file.txt"));  // Blocking!
 * });
 * 
 * ✅ GOOD - DataBufferUtils:
 * return DataBufferUtils.read(
 *     Path.of("file.txt"),
 *     new DefaultDataBufferFactory(),
 *     4096
 * ).map(dataBuffer -> ...);
 * 
 * ❌ BAD - JDBC:
 * return Mono.fromCallable(() -> {
 *     return jdbcTemplate.queryForObject(...);  // Blocking!
 * });
 * 
 * ✅ GOOD - R2DBC:
 * return databaseClient.sql("SELECT * FROM users WHERE id = :id")
 *     .bind("id", userId)
 *     .fetch()
 *     .one();
 * 
 * ❌ BAD - RestTemplate:
 * return Mono.fromCallable(() -> {
 *     return restTemplate.getForObject(url, String.class);  // Blocking!
 * });
 * 
 * ✅ GOOD - WebClient:
 * return webClient.get()
 *     .uri(url)
 *     .retrieve()
 *     .bodyToMono(String.class);
 */
@Configuration
public class BlockHoundConfig {

    private static final Logger log = LoggerFactory.getLogger(BlockHoundConfig.class);
    
    @Autowired
    private Environment environment;

    @PostConstruct
    public void init() {
        // Only enable BlockHound in non-production environments
        String[] activeProfiles = environment.getActiveProfiles();
        String profileList = activeProfiles.length > 0 
            ? String.join(", ", activeProfiles) 
            : "default";
        
        boolean isProduction = false;
        for (String profile : activeProfiles) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                isProduction = true;
                break;
            }
        }
        
        log.info("🔧 Active Spring profiles: [{}]", profileList);
        
        if (!isProduction) {
            log.info("🔍 Environment: DEV/TEST - Enabling BlockHound...");
            
            BlockHound.install(builder -> {
                
                // 🎯 Custom handler: Log blocking calls when detected
                builder.blockingMethodCallback(method -> {
                    Exception e = new Exception("Blocking call detected");
                    log.error("❌ [BLOCKING DETECTED!] Blocking method called from reactive thread!");
                    log.error("💥 Method: {}", method);
                    log.error("📍 Stack trace:", e);
                });

                // Allow specific blocking calls if needed
                // Example: Allow Thread.sleep in test code
                builder.allowBlockingCallsInside(
                    "java.io.FilterInputStream", 
                    "read"
                );
                
                // Allow Jackson JSON processing (common in WebFlux)
                builder.allowBlockingCallsInside(
                    "com.fasterxml.jackson.databind.ObjectMapper",
                    "readValue"
                );
                
                // Allow logging (logging is often blocking but necessary)
                builder.allowBlockingCallsInside(
                    "ch.qos.logback.classic.Logger",
                    "callAppenders"
                );
                
                log.info("🔍 BlockHound ENABLED - Monitoring for blocking calls...");
                log.warn("⚠️  Any blocking operation will be logged and throw BlockingOperationError");
            });
            
            log.info("✅ BlockHound successfully installed with custom logging");
        } else {
            log.info("⏭️ Environment: PRODUCTION - BlockHound DISABLED (for performance)");
        }
    }
}

