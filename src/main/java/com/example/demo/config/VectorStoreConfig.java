package com.example.demo.config;

import com.example.demo.service.HuggingFaceEmbeddingModelAdapter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI Vector Store Configuration.
 * Uses SimpleVectorStore with Hugging Face embeddings via adapter.
 */
@Configuration
public class VectorStoreConfig {

    /**
     * Create SimpleVectorStore with Hugging Face embedding model adapter.
     * SimpleVectorStore is an in-memory vector store suitable for development/demo.
     * For production, consider using PgVectorStore, Neo4jVectorStore, or PineconeVectorStore.
     */
    @Bean
    public VectorStore vectorStore(HuggingFaceEmbeddingModelAdapter embeddingModel) {
        System.out.println("🗄️ Initializing SimpleVectorStore with HuggingFace embeddings");
        System.out.println("   📏 Dimension: " + embeddingModel.dimensions());
        return new SimpleVectorStore(embeddingModel);
    }
}
