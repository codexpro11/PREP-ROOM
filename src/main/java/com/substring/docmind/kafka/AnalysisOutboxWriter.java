package com.substring.docmind.kafka;

import com.substring.docmind.interview.InterviewDtos.AnalysisRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class AnalysisOutboxWriter {
    private final AnalysisOutboxRepository repository;

    // Synchronous listener joins finish()'s transaction; rollback removes both changes.
    @EventListener
    public void enqueue(AnalysisRequested request) { repository.save(new AnalysisOutbox(request)); }
}
