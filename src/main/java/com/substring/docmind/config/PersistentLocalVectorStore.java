package com.substring.docmind.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** Small local development store, persisted after every successful mutation. */
public class PersistentLocalVectorStore extends SimpleVectorStore {
    private final Path file;

    public PersistentLocalVectorStore(EmbeddingModel model, Path file) {
        super(SimpleVectorStore.builder(model));
        this.file = file.toAbsolutePath();
        if (Files.exists(this.file)) load(this.file.toFile());
    }

    @Override public synchronized void doAdd(List<Document> documents) {
        super.doAdd(documents);
        persist();
    }

    @Override public synchronized void doDelete(List<String> ids) {
        super.doDelete(ids);
        persist();
    }

    @Override public synchronized void doDelete(Filter.Expression filter) {
        // SimpleVectorStore delegates filtered deletion to doDelete(ids), which persists once.
        super.doDelete(filter);
    }

    @Override public synchronized List<Document> doSimilaritySearch(SearchRequest request) {
        return super.doSimilaritySearch(request);
    }

    private void persist() {
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "vectors-", ".json");
            save(temporary.toFile());
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not persist the local vector store", e);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }
}
