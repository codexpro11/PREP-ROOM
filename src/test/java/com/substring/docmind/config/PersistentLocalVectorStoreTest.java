package com.substring.docmind.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersistentLocalVectorStoreTest {
    @TempDir Path directory;

    @Test void retainsOwnerScopedChunksAcrossRestartAndDeletesOnlySelectedDocument() {
        EmbeddingModel embeddings = mock(EmbeddingModel.class);
        when(embeddings.embed(any(Document.class))).thenReturn(new float[] { 1, 0 });
        when(embeddings.embed(any(String.class))).thenReturn(new float[] { 1, 0 });
        Path file = directory.resolve("index.json");
        var store = new PersistentLocalVectorStore(embeddings, file);
        store.add(List.of(new Document("First document", Map.of("userId", "1", "documentId", "a")),
                new Document("Another user", Map.of("userId", "2", "documentId", "b"))));

        var restored = new PersistentLocalVectorStore(embeddings, file);
        var filter = new FilterExpressionBuilder().eq("userId", "1").build();
        var request = SearchRequest.builder().query("document").topK(10).filterExpression(filter).build();
        assertThat(restored.similaritySearch(request)).extracting(Document::getText).containsExactly("First document");
        restored.delete(new FilterExpressionBuilder().and(
                new FilterExpressionBuilder().eq("userId", "1"),
                new FilterExpressionBuilder().eq("documentId", "a")).build());

        var afterDeletion = new PersistentLocalVectorStore(embeddings, file);
        assertThat(afterDeletion.similaritySearch(request)).isEmpty();
        assertThat(afterDeletion.similaritySearch(SearchRequest.builder().query("document").topK(10).build()))
                .extracting(Document::getText).containsExactly("Another user");
    }
}
