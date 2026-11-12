package com.substring.docmind.interview;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InterviewerRateLimiterTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

    private AnnotationConfigApplicationContext context(Map<String, Object> properties) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("test", properties));
        context.registerBean(StringRedisTemplate.class, () -> redis);
        context.register(InterviewerRateLimiter.class);
        context.refresh();
        // Exclude Spring bean lifecycle callbacks from request interaction assertions.
        clearInvocations(redis);
        return context;
    }

    @Test void disabledLimiterNeverContactsRedis() {
        try (var context = context(Map.of("app.rate-limit.enabled", "false"))) {
            context.getBean(InterviewerRateLimiter.class).check(42L);
            verifyNoInteractions(redis);
        }
    }

    @Test void enabledByDefaultAllowsRequest() {
        when(redis.execute(any(RedisScript.class), eq(List.of("preproom:limit:create:42")),
                eq("5"), eq("60"))).thenReturn(1L);
        try (var context = context(Map.of())) {
            context.getBean(InterviewerRateLimiter.class).check(42L);
            verify(redis).execute(any(RedisScript.class), eq(List.of("preproom:limit:create:42")),
                    eq("5"), eq("60"));
        }
    }

    @Test void exhaustedLimitReturns429() {
        when(redis.execute(any(RedisScript.class), anyList(), eq("5"), eq("60"))).thenReturn(0L);
        try (var context = context(Map.of())) {
            assertThatThrownBy(() -> context.getBean(InterviewerRateLimiter.class).check(42L))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            exception -> assertThat(exception.getStatusCode().value()).isEqualTo(429));
        }
    }

    @Test void redisOutageStillReturns503WhenEnabled() {
        when(redis.execute(any(RedisScript.class), anyList(), eq("5"), eq("60")))
                .thenThrow(new RedisConnectionFailureException("Redis unavailable"));
        try (var context = context(Map.of("app.rate-limit.enabled", "true"))) {
            assertThatThrownBy(() -> context.getBean(InterviewerRateLimiter.class).check(42L))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            exception -> assertThat(exception.getStatusCode().value()).isEqualTo(503));
        }
    }

    @Test void missingRedisResultReturns503() {
        try (var context = context(Map.of())) {
            assertThatThrownBy(() -> context.getBean(InterviewerRateLimiter.class).check(42L))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            exception -> assertThat(exception.getStatusCode().value()).isEqualTo(503));
        }
    }
}
