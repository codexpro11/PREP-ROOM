package com.substring.docmind.interview;

import com.substring.docmind.entity.*;
import com.substring.docmind.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.springframework.http.*;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static com.substring.docmind.interview.InterviewDtos.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.kafka.enabled=false", "spring.kafka.admin.auto-create=false",
        "spring.datasource.url=jdbc:h2:mem:interviews;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.ai.vectorstore.pgvector.initialize-schema=false",
        "spring.ai.google.genai.api-key=test-only", "spring.ai.google.genai.embedding.api-key=test-only",
        "jwt.secret=test-only-key-with-at-least-thirty-two-bytes-long" })
class InterviewIntegrationTest {
    @LocalServerPort int port;
    @MockitoBean InterviewAi ai;
    @MockitoBean VectorStore vectors;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired DocumentMetadataRepo documents;
    @Autowired InterviewService sessions;
    @Autowired InterviewRepository interviews;
    private RestClient client;

    @BeforeEach void setup() {
        client = RestClient.builder().baseUrl("http://localhost:" + port + "/api/v1").build();
        when(ai.question(any())).thenReturn(new InterviewAi.Question("Tell me about a project and your contribution.", null));
        when(ai.feedback(any())).thenReturn(new Feedback(80, 85, null, 75, "A clear example with room for detail.",
                List.of("Clear ownership"), List.of("Quantify the outcome"), List.of("Practice a concise STAR example"),
                "I delivered [specific outcome] by [your action].", "Not assessed: typed answers contain no audio evidence."));
    }
    String register() {
        String username = "test" + UUID.randomUUID().toString().substring(0, 8);
        client.post().uri("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "email", username + "@example.test", "password", "test-password-123")).retrieve().toBodilessEntity();
        String response = client.post().uri("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "password", "test-password-123")).retrieve().body(String.class);
        return json.readTree(response).get("accessToken").asText();
    }
    Detail start(String token) {
        return client.post().uri("/interviews").headers(h -> h.setBearerAuth(token)).contentType(MediaType.APPLICATION_JSON)
                .body(new Setup("Java Developer", Interview.Mode.INTERVIEW, Interview.Style.FRIENDLY, Interview.Level.INTERMEDIATE, 3, null))
                .retrieve().body(Detail.class);
    }
    int status(String method, String uri, String token, Object body) {
        return client.method(HttpMethod.valueOf(method)).uri(uri).headers(h -> { if (token != null) h.setBearerAuth(token); })
                .contentType(MediaType.APPLICATION_JSON).body(body == null ? Map.of() : body)
                .exchange((request, response) -> response.getStatusCode().value());
    }
    @Test void authenticatedLifecycleDeduplicatesAnswersAndExportsReport() throws Exception {
        String token = register(); Detail s = start(token); String base = "/interviews/" + s.id();
        Answer answer = new Answer("I built a Spring Boot service, added validation, and reduced failed requests by 20 percent.", UUID.randomUUID());
        Detail first = client.post().uri(base + "/answers").headers(h -> h.setBearerAuth(token)).body(answer).retrieve().body(Detail.class);
        Detail retry = client.post().uri(base + "/answers").headers(h -> h.setBearerAuth(token)).body(answer).retrieve().body(Detail.class);
        assertThat(first.transcripts()).hasSize(3); assertThat(retry.transcripts()).hasSize(3);
        assertThat(status("POST", base + "/finish", token, null)).isEqualTo(200);
        Detail finished = null;
        for (int n = 0; n < 60; n++) {
            finished = client.get().uri(base).headers(h -> h.setBearerAuth(token)).retrieve().body(Detail.class);
            if (finished.status() == Interview.Status.ANALYZED) break;
            Thread.sleep(100);
        }
        assertThat(finished.status()).isEqualTo(Interview.Status.ANALYZED);
        assertThat(finished.feedback().fluencyScore()).isNull();
        assertThat(status("POST", base + "/answers", token, new Answer("Another answer", UUID.randomUUID()))).isEqualTo(409);
        byte[] pdf = client.get().uri(base + "/report").headers(h -> h.setBearerAuth(token)).retrieve().body(byte[].class);
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
    @Test void anotherUserCannotReadMutateExportOrAttachPrivateResources() {
        String owner = register(), stranger = register(); Detail s = start(owner); String base = "/interviews/" + s.id();
        assertThat(status("GET", base, null, null)).isEqualTo(401);
        assertThat(status("GET", base, stranger, null)).isEqualTo(404);
        assertThat(status("GET", base + "/report", stranger, null)).isEqualTo(404);
        assertThat(status("POST", base + "/finish", stranger, null)).isEqualTo(404);
        assertThat(status("POST", base + "/answers", stranger, new Answer("test", UUID.randomUUID()))).isEqualTo(404);
        var document = DocumentMetadata.builder().filename("Private company.txt").contentType("text/plain").status(DocumentStatus.INDEXED)
                .createdAt(java.time.LocalDateTime.now()).user(users.findAll().getFirst()).build();
        document = documents.save(document);
        assertThat(status("GET", "/documents/" + document.getId(), stranger, null)).isEqualTo(404);
        assertThat(status("DELETE", "/documents/" + document.getId(), stranger, null)).isEqualTo(404);
        assertThat(status("POST", "/interviews", stranger, new Setup("Engineer", Interview.Mode.INTERVIEW, Interview.Style.FRIENDLY,
                Interview.Level.BEGINNER, 3, document.getId()))).isEqualTo(404);
    }
    @Test void rejectsInvalidInputsAndEmptyFeedback() {
        String token = register(); Detail s = start(token);
        assertThat(status("POST", "/interviews/" + s.id() + "/finish", token, null)).isEqualTo(409);
        assertThat(status("POST", "/interviews", token, new Setup(" ", Interview.Mode.INTERVIEW, Interview.Style.FRIENDLY,
                Interview.Level.BEGINNER, 100, null))).isEqualTo(400);
        assertThat(status("POST", "/interviews/" + s.id() + "/answers", token, new Answer(" ", UUID.randomUUID()))).isEqualTo(400);
    }
    @Test void communicationSettingsSurviveReloadAndAnalysisWithoutInterviewContext() {
        String token = register();
        var options = new PracticeOptions(PracticeOptions.Scenario.PRESENTATION, PracticeOptions.CommunicationGoal.STRUCTURE,
                PracticeOptions.PronunciationFocus.INTONATION, PracticeOptions.ExerciseFormat.CUSTOM, "  my college project  ", "irrelevant text");
        Detail created = client.post().uri("/interviews").headers(h -> h.setBearerAuth(token))
                .body(new Setup("stale interview role", Interview.Mode.COMMUNICATION, Interview.Style.TECHNICAL,
                        Interview.Level.INTERMEDIATE, 3, UUID.randomUUID(), options)).retrieve().body(Detail.class);
        Detail loaded = client.get().uri("/interviews/" + created.id()).headers(h -> h.setBearerAuth(token)).retrieve().body(Detail.class);
        assertThat(loaded.jobRole()).isEqualTo("Presenting an idea");
        assertThat(loaded.documentId()).isNull();
        assertThat(loaded.practiceOptions().scenario()).isEqualTo(PracticeOptions.Scenario.PRESENTATION);
        assertThat(loaded.practiceOptions().communicationGoal()).isEqualTo(PracticeOptions.CommunicationGoal.STRUCTURE);
        assertThat(loaded.practiceOptions().topic()).isEqualTo("my college project");
        assertThat(loaded.practiceOptions().customText()).isNull();
        assertThat(loaded.practiceOptions().pronunciationFocus()).isNull();
        var stored = sessions.analysisInput(created.id(), interviews.findById(created.id()).orElseThrow().getUserId());
        assertThat(stored.getPracticeOptions()).isEqualTo(loaded.practiceOptions());
    }

    @Test void pronunciationAcceptsCustomTextWithoutRoleAndRejectsUnusableSettings() {
        String token = register();
        var options = new PracticeOptions(null, null, PracticeOptions.PronunciationFocus.INTONATION,
                PracticeOptions.ExerciseFormat.CUSTOM, "  rising questions  ", "  Are you joining us today?  ");
        Detail created = client.post().uri("/interviews").headers(h -> h.setBearerAuth(token))
                .body(new Setup(null, Interview.Mode.PRONUNCIATION, Interview.Style.FRIENDLY,
                        Interview.Level.BEGINNER, 3, null, options)).retrieve().body(Detail.class);
        Detail loaded = client.get().uri("/interviews/" + created.id()).headers(h -> h.setBearerAuth(token)).retrieve().body(Detail.class);
        assertThat(loaded.jobRole()).isEqualTo("Intonation");
        assertThat(loaded.practiceOptions().customText()).isEqualTo("Are you joining us today?");
        assertThat(loaded.practiceOptions().topic()).isEqualTo("rising questions");
        when(ai.audio(any(), any(), any())).thenReturn(new AudioResult("Are you joining us today?", "Use rising intonation.", 2));
        var form = new org.springframework.util.LinkedMultiValueMap<String, Object>();
        form.add("file", new org.springframework.core.io.ByteArrayResource(WavAudioTest.recording(2)) {
            @Override public String getFilename() { return "answer.wav"; }
        });
        form.add("requestId", UUID.randomUUID().toString());
        client.post().uri("/interviews/" + created.id() + "/audio").headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.MULTIPART_FORM_DATA).body(form).retrieve().body(Detail.class);
        var audioPrompt = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(ai).audio(any(), any(), audioPrompt.capture());
        assertThat(audioPrompt.getValue()).contains("Intonation", "My own text", "Are you joining us today?");
        var emptyText = new PracticeOptions(null, null, null, PracticeOptions.ExerciseFormat.CUSTOM, null, " ");
        assertThat(status("POST", "/interviews", token, new Setup(null, Interview.Mode.PRONUNCIATION,
                Interview.Style.FRIENDLY, Interview.Level.BEGINNER, 3, null, emptyText))).isEqualTo(400);
        var invalidFormat = new PracticeOptions(null, null, PracticeOptions.PronunciationFocus.INTONATION,
                PracticeOptions.ExerciseFormat.WORDS, null, null);
        assertThat(status("POST", "/interviews", token, new Setup(null, Interview.Mode.PRONUNCIATION,
                Interview.Style.FRIENDLY, Interview.Level.BEGINNER, 3, null, invalidFormat))).isEqualTo(400);
        var tooLong = new PracticeOptions(null, null, null, PracticeOptions.ExerciseFormat.CUSTOM, null, "a".repeat(1201));
        assertThat(status("POST", "/interviews", token, new Setup(null, Interview.Mode.PRONUNCIATION,
                Interview.Style.FRIENDLY, Interview.Level.BEGINNER, 3, null, tooLong))).isEqualTo(400);
        assertThat(status("POST", "/interviews", token, Map.of("mode", "COMMUNICATION", "style", "FRIENDLY",
                "level", "BEGINNER", "questionCount", 3, "practiceOptions", Map.of("scenario", "UNKNOWN")))).isEqualTo(400);
        assertThat(status("POST", "/interviews", token, new Setup(" ", Interview.Mode.INTERVIEW,
                Interview.Style.FRIENDLY, Interview.Level.BEGINNER, 3, null))).isEqualTo(400);
    }

    @Test void legacyLanguageRequestsReceiveModeDefaultsAndInterviewDiscardsLanguageSettings() {
        String token = register();
        Detail language = client.post().uri("/interviews").headers(h -> h.setBearerAuth(token))
                .body(new Setup("Engineer", Interview.Mode.COMMUNICATION, Interview.Style.FRIENDLY,
                        Interview.Level.BEGINNER, 3, null)).retrieve().body(Detail.class);
        assertThat(language.practiceOptions().scenario()).isEqualTo(PracticeOptions.Scenario.EVERYDAY);
        assertThat(language.practiceOptions().communicationGoal()).isEqualTo(PracticeOptions.CommunicationGoal.CLARITY);
        var options = new PracticeOptions(PracticeOptions.Scenario.DISCUSSION, null, null, null, "Travel", null);
        Detail interview = client.post().uri("/interviews").headers(h -> h.setBearerAuth(token))
                .body(new Setup("Engineer", Interview.Mode.INTERVIEW, Interview.Style.FRIENDLY,
                        Interview.Level.BEGINNER, 3, null, options)).retrieve().body(Detail.class);
        assertThat(interview.practiceOptions()).isNull();
        assertThat(interview.jobRole()).isEqualTo("Engineer");
    }
    @Test void failedQuestionGenerationRollsBackAnswerAndCanBeRetried() {
        String token = register(); Detail s = start(token); String base = "/interviews/" + s.id();
        when(ai.question(any())).thenThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Try again"));
        Answer answer = new Answer("I designed a reliable API", UUID.randomUUID());
        assertThat(status("POST", base + "/answers", token, answer)).isEqualTo(503);
        Detail saved = client.get().uri(base).headers(h -> h.setBearerAuth(token)).retrieve().body(Detail.class);
        assertThat(saved.transcripts()).hasSize(1);
        doReturn(new InterviewAi.Question("How did you measure reliability?", null)).when(ai).question(any());
        assertThat(status("POST", base + "/answers", token, answer)).isEqualTo(200);
    }

    @Test void finishesAtQuestionLimitAndRejectsFurtherAnswers() {
        String token = register(); Detail s = start(token); Detail turn = s;
        for (int n = 0; n < 3; n++) {
            turn = client.post().uri("/interviews/" + s.id() + "/answers").headers(h -> h.setBearerAuth(token))
                    .body(new Answer("My answer " + n, UUID.randomUUID())).retrieve().body(Detail.class);
        }
        assertThat(turn.status()).isEqualTo(Interview.Status.COMPLETED);
        assertThat(turn.transcripts()).hasSize(6);
        assertThat(status("POST", "/interviews/" + s.id() + "/answers", token, new Answer("extra", UUID.randomUUID()))).isEqualTo(409);
    }

    @Test void acceptsVoiceAnswerWithServerDerivedDurationAndRejectsInvalidAudio() {
        String token = register(); Detail s = start(token);
        when(ai.audio(any(), any(), any())).thenReturn(new AudioResult("I built a Java API.", "Pause between your main ideas.", 30));
        var form = new org.springframework.util.LinkedMultiValueMap<String, Object>();
        form.add("file", new org.springframework.core.io.ByteArrayResource(WavAudioTest.recording(2)) {
            @Override public String getFilename() { return "answer.wav"; }
        });
        form.add("requestId", UUID.randomUUID().toString());
        Detail response = client.post().uri("/interviews/" + s.id() + "/audio").headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.MULTIPART_FORM_DATA).body(form).retrieve().body(Detail.class);
        assertThat(response.transcripts().get(1).audioSeconds()).isEqualTo(2);
        assertThat(response.transcripts().get(1).audioFeedback()).contains("Pause");
        form.set("file", new org.springframework.core.io.ByteArrayResource(new byte[60]) {
            @Override public String getFilename() { return "fake.wav"; }
        });
        int status = client.post().uri("/interviews/" + s.id() + "/audio").headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.MULTIPART_FORM_DATA).body(form).exchange((request, res) -> res.getStatusCode().value());
        assertThat(status).isEqualTo(400);
    }
}
