package com.substring.docmind.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service @RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class AnalysisOutboxPublisher {
    private final AnalysisOutboxRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    @Value("${app.kafka.analysis-topic}") private String topic;

    @Transactional
    public void publish(UUID id) throws Exception {
        var entry = repository.lockById(id);
        if (entry.isEmpty()) return;
        var event = entry.get().request();
        // Delete only after broker acknowledgement. A crash here can cause redelivery.
        kafka.send(topic, event.id().toString(), json.writeValueAsString(event)).get(10, TimeUnit.SECONDS);
        repository.delete(entry.get());
    }
}
