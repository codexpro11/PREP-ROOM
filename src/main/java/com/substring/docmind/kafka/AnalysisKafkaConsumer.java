package com.substring.docmind.kafka;

import com.substring.docmind.interview.*;
import com.substring.docmind.interview.InterviewDtos.AnalysisRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.util.Objects;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class AnalysisKafkaConsumer {
    private final InterviewService sessions;
    private final InterviewAi ai;
    private final ObjectMapper json;

    public AnalysisRequested decode(String payload) {
        var request = json.readValue(payload, AnalysisRequested.class);
        if (request == null || request.id() == null || request.userId() == null || request.attempt() == null) {
            throw new IllegalArgumentException("Invalid analysis request");
        }
        return request;
    }

    @KafkaListener(topics = "${app.kafka.analysis-topic}", groupId = "${app.kafka.analysis-group}")
    public void analyze(String payload) {
        var request = decode(payload);
        var interview = sessions.analysisInput(request.id(), request.userId());
        // Kafka/outbox delivery is at least once; completed and superseded jobs are no-ops.
        if (interview.getStatus() != Interview.Status.ANALYZING
                || !Objects.equals(interview.getAnalysisAttempt(), request.attempt())) return;
        sessions.storeFeedback(request, Objects.requireNonNull(ai.feedback(interview), "Feedback is missing"));
    }

    public void failed(String payload) { sessions.storeFeedback(decode(payload), null); }
}
