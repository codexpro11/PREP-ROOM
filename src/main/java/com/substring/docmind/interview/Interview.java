package com.substring.docmind.interview;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "interviews", indexes = @Index(columnList = "userId,createdAt"))
@Getter @Setter
public class Interview {
    public enum Mode { INTERVIEW, COMMUNICATION, PRONUNCIATION }
    public enum Style { FRIENDLY, TECHNICAL, CHALLENGING }
    public enum Level { BEGINNER, INTERMEDIATE, ADVANCED }
    public enum Status { IN_PROGRESS, COMPLETED, ANALYZING, ANALYZED, ANALYSIS_FAILED }
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false, length = 120) private String jobRole;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Mode mode;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Style style;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Level level;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.IN_PROGRESS;
    private UUID documentId;
    private String documentName;
    @Convert(converter = PracticeOptionsConverter.class)
    @Column(columnDefinition = "text") private PracticeOptions practiceOptions;
    private int questionCount;
    private Instant createdAt = Instant.now();
    private Instant endedAt;
    private Instant analysisStartedAt;
    private UUID analysisAttempt;
    @Version private long version;
    @Column(columnDefinition = "text") private String feedbackJson;
    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    private List<Transcript> transcripts = new ArrayList<>();

    public void add(String speaker, String text, String audioFeedback, Double seconds, String sources, UUID requestId) {
        Transcript transcript = new Transcript();
        transcript.setInterview(this);
        transcript.setSequenceNumber(transcripts.size());
        transcript.setSpeaker(speaker);
        transcript.setContent(text);
        transcript.setAudioFeedback(audioFeedback);
        transcript.setAudioSeconds(seconds);
        transcript.setSources(sources);
        transcript.setRequestId(requestId);
        transcripts.add(transcript);
    }
}
