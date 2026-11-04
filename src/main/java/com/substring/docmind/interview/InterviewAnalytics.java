package com.substring.docmind.interview;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import static com.substring.docmind.interview.InterviewDtos.*;

@Component @RequiredArgsConstructor @Slf4j
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class InterviewAnalytics {
    private final InterviewService sessions;
    private final InterviewAi ai;

    @Async("interviewExecutor")
    @TransactionalEventListener
    public void analyze(AnalysisRequested request) {
        try {
            var feedback = ai.feedback(sessions.analysisInput(request.id(), request.userId()));
            sessions.storeFeedback(request, feedback);
        } catch (Exception e) {
            log.warn("Interview analysis failed for {} ({})", request.id(), e.getClass().getSimpleName());
            sessions.storeFeedback(request, null);
        }
    }
}
