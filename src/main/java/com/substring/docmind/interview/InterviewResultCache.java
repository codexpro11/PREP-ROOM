package com.substring.docmind.interview;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewResultCache
{
    private final StringRedisTemplate redis;
    private final JsonMapper json;

    private String key(Long userId, UUID interviewId) {
        return "preproom:interview:v1:" + userId + ":" + interviewId;
    }

    public InterviewDtos.Detail getOrLoad(
            Long userId,
            UUID interviewId,
            Supplier<InterviewDtos.Detail> loader
    ) {
        String key = key(userId, interviewId);

        try {
            String cached = redis.opsForValue().get(key);

            if (cached != null) {
                try {
                    return json.readValue(cached, InterviewDtos.Detail.class);
                } catch (RuntimeException exception) {
                    log.warn("Invalid interview cache entry; loading from database");
                }
            }
        } catch (DataAccessException exception) {
            log.warn("Redis read failed; loading from database");
        }

        InterviewDtos.Detail result = loader.get();

        // Never cache active interviews or pending analysis.
        if (result.status() == Interview.Status.ANALYZED) {
            try {
                String value = json.writeValueAsString(result);
                redis.opsForValue().set(key, value, Duration.ofMinutes(10));
            } catch (RuntimeException exception) {
                log.warn("Interview cache write failed; returning database result");
            }
        }

        return result;
    }

    public void evict(Long userId, UUID interviewId) {
        redis.delete(key(userId, interviewId));
    }
}
