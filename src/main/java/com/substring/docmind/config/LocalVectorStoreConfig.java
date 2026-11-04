package com.substring.docmind.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class LocalVectorStoreConfig {
    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel,
                            @Value("${app.vector-store.file:./data/vector-store.json}") String file) {
        return new PersistentLocalVectorStore(embeddingModel, Path.of(file));
    }
}
