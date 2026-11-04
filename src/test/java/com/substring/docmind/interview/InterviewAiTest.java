package com.substring.docmind.interview;

import com.substring.docmind.repository.DocumentMetadataRepo;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class InterviewAiTest {
    @Test void languageQuestionsReceiveTheirOwnSettingsAndDoNotRetrieveCompanyContext() {
        ChatModel model = mock(ChatModel.class); VectorStore vectors = mock(VectorStore.class);
        when(model.getOptions()).thenReturn(org.springframework.ai.chat.prompt.ChatOptions.builder().build());
        when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Let's practise.")))));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var ai = new InterviewAi(model, vectors, mock(DocumentMetadataRepo.class), factory.getValidator());
            Interview i = new Interview(); i.setMode(Interview.Mode.COMMUNICATION); i.setJobRole("stale Engineer role");
            i.setDocumentId(UUID.randomUUID()); i.setLevel(Interview.Level.BEGINNER); i.setStyle(Interview.Style.TECHNICAL);
            i.setPracticeOptions(PracticeOptions.normalize(i.getMode(), new PracticeOptions(PracticeOptions.Scenario.EVERYDAY,
                    PracticeOptions.CommunicationGoal.LISTENING, null, null, "Travel", null)));
            ai.question(i);
            var captor = ArgumentCaptor.forClass(Prompt.class); verify(model).call(captor.capture());
            assertThat(captor.getValue().getUserMessage().getText()).contains("Everyday conversation", "Respond & ask follow-up questions", "Travel")
                    .doesNotContain("Engineer");
            verifyNoInteractions(vectors);
            clearInvocations(model);
            i.setMode(Interview.Mode.PRONUNCIATION);
            i.setPracticeOptions(PracticeOptions.normalize(i.getMode(), new PracticeOptions(null, null,
                    PracticeOptions.PronunciationFocus.INTONATION, PracticeOptions.ExerciseFormat.CUSTOM, null, "Are you ready?")));
            ai.question(i); verify(model).call(captor.capture());
            assertThat(captor.getValue().getUserMessage().getText()).contains("Intonation", "My own text", "Are you ready?")
                    .doesNotContain("Engineer");
        }
    }

    @Test void languageFeedbackUsesTheGoalAndCannotClaimStarOrTextOnlySpeechScores() {
        ChatModel model = mock(ChatModel.class);
        when(model.getOptions()).thenReturn(org.springframework.ai.chat.prompt.ChatOptions.builder().build());
        String response = """
                {"relevanceScore":80,"grammarScore":80,"fluencyScore":99,"starScore":100,
                "summary":"A useful response","strengths":["Clear answer"],"improvements":["More detail"],
                "practicePlan":["Practice daily"],"exampleAnswer":"For example...","pronunciationNotes":"Perfect pronunciation"}
                """;
        when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(response)))));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var ai = new InterviewAi(model, mock(VectorStore.class), mock(DocumentMetadataRepo.class), factory.getValidator());
            for (var mode : List.of(Interview.Mode.COMMUNICATION, Interview.Mode.PRONUNCIATION)) {
                Interview i = new Interview(); i.setMode(mode); i.setJobRole("unused role");
                i.setPracticeOptions(PracticeOptions.normalize(mode, null));
                i.add("CANDIDATE", "A typed response.", null, null, null, UUID.randomUUID());
                var feedback = ai.feedback(i);
                assertThat(feedback.starScore()).isNull(); assertThat(feedback.fluencyScore()).isNull();
                assertThat(feedback.pronunciationNotes()).contains("Not assessed");
            }
            var captor = ArgumentCaptor.forClass(Prompt.class); verify(model, times(2)).call(captor.capture());
            assertThat(captor.getAllValues().get(0).getUserMessage().getText()).contains("Express ideas clearly");
            assertThat(captor.getAllValues().get(1).getUserMessage().getText()).contains("Word stress", "Short sentences");
        }
    }
    @Test void companyRetrievalIsScopedToBothOwnerAndSelectedDocument() {
        ChatModel model = mock(ChatModel.class); VectorStore vectors = mock(VectorStore.class);
        when(model.getOptions()).thenReturn(org.springframework.ai.chat.prompt.ChatOptions.builder().build());
        DocumentMetadataRepo documents = mock(DocumentMetadataRepo.class);
        Interview i = new Interview(); i.setUserId(17L); i.setDocumentId(UUID.randomUUID()); i.setDocumentName("Company guide");
        i.setJobRole("Engineer"); i.setMode(Interview.Mode.INTERVIEW); i.setStyle(Interview.Style.FRIENDLY); i.setLevel(Interview.Level.BEGINNER);
        when(documents.existsById(i.getDocumentId())).thenReturn(true);
        when(vectors.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document("We use Java and Spring Boot.")));
        when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("How would you design a Java service?")))));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var ai = new InterviewAi(model, vectors, documents, factory.getValidator());
            assertThat(ai.question(i).sources()).isEqualTo("Company guide");
            var captor = ArgumentCaptor.forClass(SearchRequest.class); verify(vectors).similaritySearch(captor.capture());
            String filter = captor.getValue().getFilterExpression().toString();
            assertThat(filter).contains("userId", "17", "documentId", i.getDocumentId().toString(), "AND");
        }
    }
    @Test void retrievalFailureDoesNotPretendToProduceGroundedQuestions() {
        ChatModel model = mock(ChatModel.class); VectorStore vectors = mock(VectorStore.class); DocumentMetadataRepo docs = mock(DocumentMetadataRepo.class);
        Interview i = new Interview(); i.setUserId(1L); i.setDocumentId(UUID.randomUUID()); i.setJobRole("Engineer"); i.setMode(Interview.Mode.INTERVIEW);
        when(docs.existsById(i.getDocumentId())).thenReturn(true);
        when(vectors.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThatThrownBy(() -> new InterviewAi(model, vectors, docs, factory.getValidator()).question(i))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("503");
            verifyNoInteractions(model);
        }
    }
    @Test void textOnlyFeedbackCannotClaimSpokenFluencyOrPronunciation() {
        ChatModel model = mock(ChatModel.class);
        when(model.getOptions()).thenReturn(org.springframework.ai.chat.prompt.ChatOptions.builder().build());
        String response = """
                {"relevanceScore":80,"grammarScore":80,"fluencyScore":99,"starScore":null,
                "summary":"A useful response","strengths":["Clear answer"],"improvements":["More detail"],
                "practicePlan":["Practice daily"],"exampleAnswer":"For example...","pronunciationNotes":"Perfect pronunciation"}
                """;
        when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(response)))));
        Interview i = new Interview(); i.add("CANDIDATE", "I delivered the project.", null, null, null, UUID.randomUUID());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var f = new InterviewAi(model, mock(VectorStore.class), mock(DocumentMetadataRepo.class), factory.getValidator()).feedback(i);
            assertThat(f.fluencyScore()).isNull(); assertThat(f.pronunciationNotes()).contains("Not assessed");
        }
    }
}
