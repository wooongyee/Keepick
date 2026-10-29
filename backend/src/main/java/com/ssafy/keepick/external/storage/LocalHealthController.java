package com.ssafy.keepick.external.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
public class LocalHealthController {
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    @GetMapping("/api/local-health")
    public Map<String, Object> health() {
        jdbc.queryForObject("SELECT 1", Integer.class);
        String ping = redis.execute((org.springframework.data.redis.core.RedisCallback<String>) connection -> connection.ping());
        if (!"PONG".equals(ping)) throw new IllegalStateException("Redis unavailable");
        return Map.of("status", "ok", "database", "ok", "redis", "ok", "storage", "local");
    }
}
