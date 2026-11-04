package com.substring.docmind.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @Slf4j
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class AnalysisOutboxRelay {
    private final AnalysisOutboxRepository repository;
    private final AnalysisOutboxPublisher publisher;

    @Scheduled(fixedDelayString = "${app.kafka.outbox-poll-ms:2000}")
    public void relay() {
        for (var entry : repository.findTop20ByOrderByCreatedAtAsc()) {
            try { publisher.publish(entry.getId()); }
            catch (Exception e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                log.warn("Kafka publish failed; retaining analysis job {} ({})", entry.getId(), e.getClass().getSimpleName());
                return;
            }
        }
    }
}
