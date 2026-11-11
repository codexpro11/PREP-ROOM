package com.substring.docmind.interview;

import com.substring.docmind.entity.User;
import com.substring.docmind.entity.DocumentStatus;
import com.substring.docmind.repository.DocumentMetadataRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.Duration;
import java.util.*;
import static com.substring.docmind.interview.InterviewDtos.*;

@Service @RequiredArgsConstructor
public class InterviewService {
    private final InterviewRepository repository;
    private final DocumentMetadataRepo documents;
    private final InterviewAi ai;
    private final ObjectMapper json;
    private final ApplicationEventPublisher events;
    private final InterviewResultCache resultCache;

    @Transactional
    public Detail create(Setup setup, User user) {
        Interview interview = new Interview();
        interview.setUserId(user.getId());
        var options = PracticeOptions.normalize(setup.mode(), setup.practiceOptions());
        interview.setPracticeOptions(options);
        if (setup.mode() == Interview.Mode.INTERVIEW && (setup.jobRole() == null || setup.jobRole().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a target job role for a mock interview.");
        }
        interview.setJobRole(options == null ? setup.jobRole().trim() : options.title(setup.mode()));
        interview.setMode(setup.mode());
        interview.setStyle(setup.style());
        interview.setLevel(setup.level());
        interview.setQuestionCount(setup.questionCount());
        if (setup.mode() == Interview.Mode.INTERVIEW && setup.documentId() != null) {
            var document = documents.findByIdAndUser(setup.documentId(), user).orElseThrow(this::notFound);
            if (document.getStatus() != DocumentStatus.INDEXED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Wait for this document to finish indexing.");
            }
            interview.setDocumentId(document.getId());
            interview.setDocumentName(document.getFilename());
        }
        // The first read-aloud exercise is ready immediately; later turns use AI and the recorded evidence.
        var question = setup.mode() == Interview.Mode.PRONUNCIATION
                ? new InterviewAi.Question(PronunciationExercises.first(options), null)
                : ai.question(interview);
        interview.add("AI", question.text(), null, null, question.sources(), null);
        return view(repository.saveAndFlush(interview));
    }

    @Transactional(readOnly = true)
    public List<Summary> list(User user) {
        return repository.findTop100ByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(i ->
                new Summary(i.getId(), i.getJobRole(), i.getMode(), i.getStyle(), i.getStatus(),
                        answers(i), i.getQuestionCount(), i.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public Detail get(UUID id, User user) {
        Long userId = user.getId();

        // Check ownership on every request, including cache hits.
        if (!repository.existsByIdAndUserId(id, userId)) {
            throw notFound();
        }

        return resultCache.getOrLoad(userId, id, () ->
                view(repository.findByIdAndUserId(id, userId)
                        .orElseThrow(this::notFound))
        );
    }

    @Transactional
    public Detail answer(UUID id, User user, Answer answer) {
        return accept(repository.lockOwned(id, user.getId()).orElseThrow(this::notFound),
                answer.text().trim(), null, null, answer.requestId());
    }

    @Transactional
    public Detail audio(UUID id, User user, byte[] bytes, String mime, UUID requestId) {
        Interview i = repository.lockOwned(id, user.getId()).orElseThrow(this::notFound);
        if (duplicate(i, requestId)) return view(i);
        requireActive(i);
        double duration = WavAudio.duration(bytes);
        var result = ai.audio(bytes, mime, InterviewAi.practiceData(i) + "\nCurrent exercise:\n" + i.getTranscripts().getLast().getContent());
        return accept(i, result.text(), result.coaching(), duration, requestId);
    }

    private Detail accept(Interview i, String text, String coaching, Double seconds, UUID requestId) {
        // The row lock serializes REST/audio turns and finish. Retries reuse a request ID.
        if (duplicate(i, requestId)) return view(i);
        requireActive(i);
        i.add("CANDIDATE", text, coaching, seconds, null, requestId);
        if (answers(i) >= i.getQuestionCount()) {
            i.setStatus(Interview.Status.COMPLETED);
            i.setEndedAt(Instant.now());
        } else {
            var question = ai.question(i);
            i.add("AI", question.text(), null, null, question.sources(), null);
        }
        repository.saveAndFlush(i);
        return view(i);
    }

    @Transactional
    public Detail finish(UUID id, User user) {
        Interview i = repository.lockOwned(id, user.getId()).orElseThrow(this::notFound);
        if (answers(i) == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Answer at least one question before requesting feedback.");
        if (i.getStatus() == Interview.Status.ANALYZED) return view(i);
        if (i.getStatus() == Interview.Status.ANALYZING && i.getAnalysisStartedAt() != null
                && Duration.between(i.getAnalysisStartedAt(), Instant.now()).toMinutes() < 5) return view(i);
        i.setStatus(Interview.Status.ANALYZING);
        if (i.getEndedAt() == null) i.setEndedAt(Instant.now());
        i.setAnalysisStartedAt(Instant.now());
        i.setAnalysisAttempt(UUID.randomUUID());
        repository.saveAndFlush(i);
        events.publishEvent(new AnalysisRequested(i.getId(), user.getId(), i.getAnalysisAttempt()));
        return view(i);
    }

    @Transactional(readOnly = true, propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Interview analysisInput(UUID id, Long userId) {
        Interview i = repository.findByIdAndUserId(id, userId).orElseThrow(this::notFound);
        i.getTranscripts().size();
        return i;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void storeFeedback(AnalysisRequested event, Feedback feedback) {
        var i = repository.lockOwned(event.id(), event.userId()).orElseThrow(this::notFound);
        if (!Objects.equals(event.attempt(), i.getAnalysisAttempt())
                || i.getStatus() != Interview.Status.ANALYZING) return;
        i.setFeedbackJson(feedback == null ? null : json.writeValueAsString(feedback));
        i.setStatus(feedback == null ? Interview.Status.ANALYSIS_FAILED : Interview.Status.ANALYZED);
    }

    private boolean duplicate(Interview i, UUID requestId) {
        return i.getTranscripts().stream().anyMatch(t -> requestId.equals(t.getRequestId()));
    }
    private int answers(Interview i) { return (int) i.getTranscripts().stream().filter(t -> t.getSpeaker().equals("CANDIDATE")).count(); }
    private void requireActive(Interview i) {
        if (i.getStatus() != Interview.Status.IN_PROGRESS) throw new ResponseStatusException(HttpStatus.CONFLICT, "This session has ended. Start a new practice session.");
    }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Interview or document not found."); }
    private Detail view(Interview i) {
        return new Detail(i.getId(), i.getJobRole(), i.getMode(), i.getStyle(), i.getLevel(), i.getStatus(),
                i.getDocumentId(), i.getDocumentName(), i.getQuestionCount(), i.getCreatedAt(), i.getEndedAt(),
                i.getTranscripts().stream().map(t -> new Turn(t.getId(), t.getSpeaker(), t.getContent(),
                        t.getAudioFeedback(), t.getAudioSeconds(), t.getSources(), t.getCreatedAt())).toList(),
                i.getFeedbackJson() == null ? null : json.readValue(i.getFeedbackJson(), Feedback.class), i.getPracticeOptions());
    }
}
