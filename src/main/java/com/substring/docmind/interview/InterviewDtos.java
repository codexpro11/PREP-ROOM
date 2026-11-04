package com.substring.docmind.interview;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.*;

public final class InterviewDtos {
    private InterviewDtos() {}
    public record Setup(@Size(max = 120) String jobRole,
                        @NotNull Interview.Mode mode, @NotNull Interview.Style style,
                        @NotNull Interview.Level level, @Min(3) @Max(15) int questionCount, UUID documentId,
                        @Valid PracticeOptions practiceOptions) {
        public Setup(String jobRole, Interview.Mode mode, Interview.Style style, Interview.Level level,
                     int questionCount, UUID documentId) {
            this(jobRole, mode, style, level, questionCount, documentId, null);
        }
        @AssertTrue(message = "Enter a target job role for a mock interview.")
        @JsonIgnore
        public boolean isRoleValid() { return mode != Interview.Mode.INTERVIEW || jobRole != null && !jobRole.isBlank(); }
    }
    public record Answer(@NotBlank @Size(max = 8000) String text, @NotNull UUID requestId) {}
    public record Turn(UUID id, String speaker, String content, String audioFeedback, Double audioSeconds,
                       String sources, Instant createdAt) {}
    public record Summary(UUID id, String jobRole, Interview.Mode mode, Interview.Style style,
                          Interview.Status status, int answered, int questionCount, Instant createdAt) {}
    public record Detail(UUID id, String jobRole, Interview.Mode mode, Interview.Style style, Interview.Level level,
                         Interview.Status status, UUID documentId, String documentName, int questionCount,
                         Instant createdAt, Instant endedAt, List<Turn> transcripts, Feedback feedback,
                         PracticeOptions practiceOptions) {}
    public record Feedback(@NotNull @Min(0) @Max(100) Integer relevanceScore,
                           @NotNull @Min(0) @Max(100) Integer grammarScore,
                           @Min(0) @Max(100) Integer fluencyScore,
                           @Min(0) @Max(100) Integer starScore,
                           @NotBlank @Size(max = 4000) String summary,
                           @NotEmpty @Size(max = 8) List<@NotBlank @Size(max = 1000) String> strengths,
                           @NotEmpty @Size(max = 8) List<@NotBlank @Size(max = 1000) String> improvements,
                           @NotEmpty @Size(max = 8) List<@NotBlank @Size(max = 1000) String> practicePlan,
                           @NotBlank @Size(max = 4000) String exampleAnswer,
                           @NotBlank @Size(max = 3000) String pronunciationNotes) {}
    public record AudioResult(@NotBlank @Size(max = 8000) String text,
                              @NotBlank @Size(max = 3000) String coaching,
                              @Positive @Max(185) double durationSeconds) {}
    public record AnalysisRequested(UUID id, Long userId, UUID attempt) {}
}
