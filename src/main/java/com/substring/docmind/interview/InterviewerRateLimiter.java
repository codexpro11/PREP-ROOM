package com.substring.docmind.interview;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewerRateLimiter {

    private final StringRedisTemplate redis;
    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    private static final DefaultRedisScript<Long> SCRIPT =
            new DefaultRedisScript<>("""
            local count = tonumber(redis.call('GET', KEYS[1]) or '0')

            if count >= tonumber(ARGV[1]) then
                return 0
            end

            count = redis.call('INCR', KEYS[1])

            if count == 1 then
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
            end

            return 1
            """, Long.class);

    public void check(Long userId) {
        if (!enabled) {
            return;
        }

        String key = "preproom:limit:create:" + userId;
        Long allowed;

        try {
            allowed = redis.execute(
                    SCRIPT,
                    List.of(key),
                    "5",    // Maximum attempts
                    "60"    // Window duration in seconds
            );
        } catch (DataAccessException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Interview creation is temporarily unavailable. Try again shortly."
            );
        }

        if (allowed == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to check the request limit."
            );
        }

        if (allowed == 0L) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many interview attempts. Try again in one minute."
            );

            }

        }

    }
