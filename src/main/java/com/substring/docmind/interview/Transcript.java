package com.substring.docmind.interview;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interview_transcripts", uniqueConstraints = @UniqueConstraint(columnNames = {"interview_id", "sequenceNumber"}))
@Getter @Setter
public class Transcript {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "interview_id") private Interview interview;
    private int sequenceNumber;
    @Column(nullable = false) private String speaker;
    @Column(nullable = false, columnDefinition = "text") private String content;
    @Column(columnDefinition = "text") private String audioFeedback;
    private Double audioSeconds;
    @Column(columnDefinition = "text") private String sources;
    private UUID requestId;
    private Instant createdAt = Instant.now();
}
