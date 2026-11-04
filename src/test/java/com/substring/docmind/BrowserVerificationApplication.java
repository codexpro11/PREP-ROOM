package com.substring.docmind;

import com.substring.docmind.interview.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.ai.vectorstore.VectorStore;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/** Test-only UI server: in-memory data and fixture AI, never packaged in the production jar. */
public class BrowserVerificationApplication {
    public static void main(String[] args) {
        Map.ofEntries(
                Map.entry("server.address", "127.0.0.1"), Map.entry("app.kafka.enabled", "false"),
                Map.entry("app.cors.allowed-origins", "http://localhost:3100,http://127.0.0.1:3100"),
                Map.entry("spring.kafka.admin.auto-create", "false"), Map.entry("spring.config.import", ""),
                Map.entry("server.port", "8081"), Map.entry("spring.datasource.url", "jdbc:h2:mem:browser;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"),
                Map.entry("spring.datasource.driver-class-name", "org.h2.Driver"), Map.entry("spring.datasource.username", "sa"),
                Map.entry("spring.datasource.password", ""), Map.entry("spring.jpa.hibernate.ddl-auto", "create-drop"),
                Map.entry("spring.ai.google.genai.api-key", "test-only"), Map.entry("spring.ai.google.genai.embedding.api-key", "test-only"),
                Map.entry("spring.ai.vectorstore.pgvector.initialize-schema", "false"),
                Map.entry("jwt.secret", "browser-test-only-signing-key-not-for-production"))
                .forEach(System::setProperty);
        SpringApplication.from(DocmindBackendApplication::main).with(Fixtures.class).run(args);
    }
    @TestConfiguration
    static class Fixtures {
        @Bean @Primary InterviewAi fixtureAi() {
            var ai = mock(InterviewAi.class);
            when(ai.question(any())).thenAnswer(invocation -> {
                Interview i = invocation.getArgument(0);
                var settings = PracticeOptions.normalize(i.getMode(), i.getPracticeOptions());
                String q = switch (i.getMode()) {
                    case PRONUNCIATION -> "Read this aloud: " + (settings.customText() == null ? "Are you joining us today?" : settings.customText())
                            + "\n\nFocus: " + settings.pronunciationFocus().label + ". Pause between ideas, then record your voice.";
                    case COMMUNICATION -> "Let's practise " + settings.scenario().label.toLowerCase()
                            + ". Explain " + (settings.topic() == null ? "something you enjoy" : settings.topic())
                            + " in a few sentences. Your goal: " + settings.communicationGoal().label.toLowerCase() + ".";
                    case INTERVIEW -> i.getTranscripts().isEmpty() ? "Tell me about a Java project you built. What problem did it solve, and what was your contribution?" : "What trade-offs did you consider, and how did you measure the result?";
                };
                return new InterviewAi.Question(q, i.getDocumentName());
            });
            when(ai.feedback(any())).thenReturn(new InterviewDtos.Feedback(82, 86, null, 78,
                    "You explained your work clearly. Make the impact more concrete by describing how you measured the result.",
                    List.of("You explained your personal contribution.", "Your answer stayed relevant to the question."),
                    List.of("Include one measurable outcome.", "Keep the setup brief so your actions stand out."),
                    List.of("Outline a STAR story from your project.", "Practice delivering it in 90 seconds.", "Record it and listen for unnecessary filler words."),
                    "I built a Spring Boot service to address [problem]. I was responsible for [your work]. We measured success using [metric].",
                    "Not assessed: typed answers contain no audio evidence."));
            when(ai.audio(any(), any(), any())).thenReturn(new InterviewDtos.AudioResult("This is a browser test recording.", "Fixture coaching only; no real audio assessment was performed.", 2));
            return ai;
        }
        @Bean @Primary VectorStore fixtureVectors() { return mock(VectorStore.class); }
    }
}
