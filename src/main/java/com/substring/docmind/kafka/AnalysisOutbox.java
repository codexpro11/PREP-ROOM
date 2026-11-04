package com.substring.docmind.kafka;

import com.substring.docmind.interview.InterviewDtos.AnalysisRequested;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/** Persisted in the same transaction as the interview's ANALYZING state. */
@Entity
@Table(name = "analysis_outbox")
@Getter @NoArgsConstructor
public class AnalysisOutbox {
    @Id private UUID id;
    @Column(nullable = false) private UUID interviewId;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private Instant createdAt;

    public AnalysisOutbox(AnalysisRequested request) {
        id = request.attempt();
        interviewId = request.id();
        userId = request.userId();
        createdAt = Instant.now();
    }

    public AnalysisRequested request() { return new AnalysisRequested(interviewId, userId, id); }
}
