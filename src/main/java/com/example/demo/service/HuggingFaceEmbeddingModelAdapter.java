package com.example.demo.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Adapter to bridge HuggingFaceEmbeddingService to Spring AI's EmbeddingModel interface.
 * Allows using HuggingFace API with Spring AI's VectorStore ecosystem.
 */
@Component
public class HuggingFaceEmbeddingModelAdapter implements EmbeddingModel {

    private final HuggingFaceEmbeddingService huggingFaceService;

    public HuggingFaceEmbeddingModelAdapter(HuggingFaceEmbeddingService huggingFaceService) {
        this.huggingFaceService = huggingFaceService;
        System.out.println("✅ HuggingFaceEmbeddingModelAdapter initialized");
    }

    @Override
    public float[] embed(Document document) {
        try {
            String text = document.getContent();
            return huggingFaceService.embed(text);
        } catch (Exception e) {
            throw new RuntimeException("Failed to embed document", e);
        }
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        try {
            // Extract texts from request instructions
            List<String> texts = new ArrayList<>();
            for (Object instruction : request.getInstructions()) {
                if (instruction instanceof String) {
                    texts.add((String) instruction);
                } else if (instruction instanceof Document) {
                    texts.add(((Document) instruction).getContent());
                } else {
                    texts.add(instruction.toString());
                }
            }

            System.out.println("🔄 Embedding " + texts.size() + " texts via HuggingFace adapter...");

            // Call Hugging Face service
            List<float[]> embeddings = huggingFaceService.embedBatch(texts);

            // Convert to Spring AI Embedding objects
            List<org.springframework.ai.embedding.Embedding> embeddingList = new ArrayList<>();
            for (int i = 0; i < embeddings.size(); i++) {
                float[] vector = embeddings.get(i);
                
                // Spring AI Embedding constructor: Embedding(float[] embedding, Integer index)
                embeddingList.add(new org.springframework.ai.embedding.Embedding(vector, i));
            }

            System.out.println("✅ Generated " + embeddingList.size() + " embeddings");

            // Create response
            return new EmbeddingResponse(embeddingList);

        } catch (Exception e) {
            System.err.println("❌ Failed to generate embeddings: " + e.getMessage());
            throw new RuntimeException("Failed to generate embeddings via Hugging Face", e);
        }
    }

    @Override
    public int dimensions() {
        return huggingFaceService.getDimension();
    }
}
