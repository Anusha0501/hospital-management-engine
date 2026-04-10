package com.hospital.appointment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Health Check", description = "APIs for checking system health")
public class HealthController {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    @Operation(summary = "Comprehensive health check", description = "Checks the health of all system components")
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        // Database health
        health.put("database", checkDatabaseHealth());
        
        // Redis health
        health.put("redis", checkRedisHealth());
        
        // Overall system health
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("application", "Hospital Appointment Engine");
        
        return ResponseEntity.ok(health);
    }

    @Operation(summary = "Database health check", description = "Checks database connectivity")
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> databaseHealth() {
        return ResponseEntity.ok(checkDatabaseHealth());
    }

    @Operation(summary = "Redis health check", description = "Checks Redis connectivity")
    @GetMapping("/redis")
    public ResponseEntity<Map<String, Object>> redisHealth() {
        return ResponseEntity.ok(checkRedisHealth());
    }

    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            dbHealth.put("status", "UP");
            dbHealth.put("database", connection.getMetaData().getDatabaseProductName());
            dbHealth.put("version", connection.getMetaData().getDatabaseProductVersion());
            dbHealth.put("url", connection.getMetaData().getURL());
        } catch (Exception e) {
            log.error("Database health check failed", e);
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        
        return dbHealth;
    }

    private Map<String, Object> checkRedisHealth() {
        Map<String, Object> redisHealth = new HashMap<>();
        
        try {
            // Test Redis connectivity
            redisTemplate.opsForValue().set("health:check:test", "test", 10, java.util.concurrent.TimeUnit.SECONDS);
            String result = (String) redisTemplate.opsForValue().get("health:check:test");
            
            if ("test".equals(result)) {
                redisHealth.put("status", "UP");
                redisHealth.put("connection", redisConnectionFactory.getConnection().getServerName());
            } else {
                redisHealth.put("status", "DOWN");
                redisHealth.put("error", "Redis read/write test failed");
            }
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            redisHealth.put("status", "DOWN");
            redisHealth.put("error", e.getMessage());
        }
        
        return redisHealth;
    }
}
