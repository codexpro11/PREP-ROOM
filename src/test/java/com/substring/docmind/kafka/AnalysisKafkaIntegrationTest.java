package com.substring.docmind.kafka;

import com.substring.docmind.entity.User;
import com.substring.docmind.interview.*;
import com.substring.docmind.repository.UserRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.*;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.*;
import java.util.function.BooleanSupplier;
import static com.substring.docmind.interview.InterviewDtos.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:kafka;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.ai.vectorstore.pgvector.initialize-schema=false",
        "spring.ai.google.genai.api-key=test-only", "spring.ai.google.genai.embedding.api-key=test-only",
        "jwt.secret=test-only-key-with-at-least-thirty-two-bytes-long",
        "app.kafka.enabled=true", "spring.kafka.admin.auto-create=true",
        "app.kafka.analysis-topic=test.analysis", "app.kafka.analysis-group=test-feedback",
        "app.kafka.partitions=1", "app.kafka.outbox-poll-ms=3600000", "app.kafka.retry-delay-ms=10",
        "logging.level.org.apache.kafka=ERROR", "logging.level.kafka=ERROR", "logging.level.state.change.logger=ERROR" })
@EmbeddedKafka(partitions = 1, topics = {"test.analysis", "test.analysis.DLT"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class AnalysisKafkaIntegrationTest {
    @MockitoBean InterviewAi ai;
    @MockitoBean VectorStore vectors;
    @Autowired InterviewService sessions;
    @Autowired InterviewRepository interviews;
    @Autowired UserRepository users;
    @Autowired AnalysisOutboxRepository outbox;
    @Autowired AnalysisOutboxRelay relay;
    @Autowired AnalysisKafkaConsumer listener;
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired ObjectMapper json;
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired ApplicationEventPublisher events;
    @Autowired PlatformTransactionManager transactions;
    private User user;

    @BeforeEach void setup() {
        String name = UUID.randomUUID().toString();
        user = users.save(User.builder().username(name).email(name + "@example.test").build());
        when(ai.question(any())).thenReturn(new InterviewAi.Question("Tell me about your project.", null));
    }

    Feedback feedback() {
        return new Feedback(80, 85, null, null, "Clear response", List.of("Ownership"), List.of("Add detail"),
                List.of("Practice"), "Example", "Not assessed for text");
    }

    UUID ready() {
        var session = sessions.create(new Setup("Developer", Interview.Mode.INTERVIEW, Interview.Style.FRIENDLY,
                Interview.Level.BEGINNER, 3, null), user);
        sessions.answer(session.id(), user, new Answer("I built a Java API.", UUID.randomUUID()));
        return session.id();
    }

    AnalysisRequested request(UUID id) {
        var i = interviews.findById(id).orElseThrow();
        return new AnalysisRequested(id, user.getId(), i.getAnalysisAttempt());
    }

    void await(BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < end) Thread.sleep(50);
        assertThat(condition.getAsBoolean()).isTrue();
    }

    @Test void committedOutboxJobReachesBrokerAndStoresFeedbackOnce() throws Exception {
        when(ai.feedback(any())).thenReturn(feedback());
        UUID id = ready();
        sessions.finish(id, user);
        assertThat(outbox.findById(request(id).attempt())).isPresent();
        relay.relay();
        await(() -> sessions.get(id, user).status() == Interview.Status.ANALYZED);
        assertThat(outbox.findById(request(id).attempt())).isEmpty();
        // Simulate the crash window after broker ack and before outbox deletion.
        listener.analyze(json.writeValueAsString(request(id)));
        sessions.storeFeedback(request(id), null);
        assertThat(sessions.get(id, user).status()).isEqualTo(Interview.Status.ANALYZED);
        verify(ai, times(1)).feedback(any());
    }

    @Test void transientAiFailureIsRetried() throws Exception {
        when(ai.feedback(any())).thenThrow(new IllegalStateException("temporary")).thenReturn(feedback());
        UUID id = ready(); sessions.finish(id, user); relay.relay();
        await(() -> sessions.get(id, user).status() == Interview.Status.ANALYZED);
        verify(ai, times(2)).feedback(any());
    }

    @Test void exhaustedRetriesMarkFailureAndPublishDeadLetter() throws Exception {
        when(ai.feedback(any())).thenThrow(new IllegalStateException("unavailable"));
        UUID id = ready(); sessions.finish(id, user);
        try (Consumer<String, String> deadLetters = deadLetters()) {
            relay.relay();
            var record = KafkaTestUtils.getSingleRecord(deadLetters, "test.analysis.DLT", Duration.ofSeconds(20));
            assertThat(json.readValue(record.value(), AnalysisRequested.class).id()).isEqualTo(id);
            await(() -> sessions.get(id, user).status() == Interview.Status.ANALYSIS_FAILED);
            verify(ai, times(3)).feedback(any());
        }
        UUID failedAttempt = request(id).attempt();
        doReturn(feedback()).when(ai).feedback(any());
        sessions.finish(id, user);
        assertThat(request(id).attempt()).isNotEqualTo(failedAttempt);
        relay.relay();
        await(() -> sessions.get(id, user).status() == Interview.Status.ANALYZED);
    }

    @Test void malformedPayloadGoesToDeadLetter() throws Exception {
        try (Consumer<String, String> deadLetters = deadLetters()) {
            kafka.send("test.analysis", "bad", "not-json").get();
            assertThat(KafkaTestUtils.getSingleRecord(deadLetters, "test.analysis.DLT", Duration.ofSeconds(20)).value())
                    .isEqualTo("not-json");
        }
    }

    Consumer<String, String> deadLetters() {
        var props = KafkaTestUtils.consumerProps(broker, UUID.randomUUID().toString(), false);
        var consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer()).createConsumer();
        broker.consumeFromAnEmbeddedTopic(consumer, true, "test.analysis.DLT");
        return consumer;
    }

    @Test void transactionRollbackDoesNotLeaveAQueuedJob() {
        UUID attempt = UUID.randomUUID();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            events.publishEvent(new AnalysisRequested(UUID.randomUUID(), user.getId(), attempt));
            assertThat(outbox.findById(attempt)).isPresent();
            tx.setRollbackOnly();
        });
        assertThat(outbox.findById(attempt)).isEmpty();
    }

    @Test void supersededAttemptDoesNotCallAi() {
        UUID id = ready(); sessions.finish(id, user);
        listener.analyze(json.writeValueAsString(new AnalysisRequested(id, user.getId(), UUID.randomUUID())));
        verify(ai, never()).feedback(any());
        outbox.deleteById(request(id).attempt());
    }
}
