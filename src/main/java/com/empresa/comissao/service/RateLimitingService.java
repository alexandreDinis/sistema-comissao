package com.empresa.comissao.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, this::newBucket);
    }

    public Bucket resolvePasswordResetBucket(String key) {
        return cache.computeIfAbsent("pwd_reset_" + key, k -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(3)
                    .refillIntervally(3, Duration.ofHours(1))
                    .build();
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });
    }

    private Bucket newBucket(String key) {
        // 10 failed attempts per 10 minutes (more reasonable for testing)
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillIntervally(10, Duration.ofMinutes(10))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
