package com.substring.docmind.kafka;

import com.substring.docmind.interview.InterviewDtos.AnalysisRequested;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnalysisOutboxPublisherTest {
    @Test void unavailableBrokerRetainsJobForLaterDelivery() {
        var repository = mock(AnalysisOutboxRepository.class);
        @SuppressWarnings("unchecked") KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        var event = new AnalysisRequested(UUID.randomUUID(), 1L, UUID.randomUUID());
        var entry = new AnalysisOutbox(event);
        when(repository.lockById(entry.getId())).thenReturn(Optional.of(entry));
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker offline")));
        var publisher = new AnalysisOutboxPublisher(repository, kafka, new ObjectMapper());
        ReflectionTestUtils.setField(publisher, "topic", "test.analysis");
        assertThatThrownBy(() -> publisher.publish(entry.getId())).hasCauseInstanceOf(IllegalStateException.class);
        verify(repository, never()).delete(any(AnalysisOutbox.class));
    }
}
