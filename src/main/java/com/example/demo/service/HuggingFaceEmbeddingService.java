package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Improved Hugging Face Embedding Service with batching and normalization.
 * Uses Hugging Face Inference API for text embeddings.
 */
@Service
public class HuggingFaceEmbeddingService {

    @Value("${huggingface.api.key}")
    private String apiKey;

    @Value("${huggingface.embedding.model:sentence-transformers/all-MiniLM-L6-v2}")
    private String model;

    private static final int BATCH_SIZE = 10; // HuggingFace recommended batch size
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("❌ WARNING: Hugging Face API key is not set!");
        } else {
            String maskedKey = apiKey.length() > 8 
                ? apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4)
                : "***";
            System.out.println("✅ HuggingFaceEmbeddingService (improved) initialized");
            System.out.println("🔑 API key loaded: " + maskedKey);
            System.out.println("📦 Using model: " + model);
        }
    }

    /**
     * Embed a single text (auto-normalized).
     */
    public float[] embed(String text) throws Exception {
        List<float[]> results = embedBatch(Collections.singletonList(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    /**
     * Embed batch of texts with automatic batching + normalization.
     * Splits large batches into smaller chunks for API limits.
     */
    public List<float[]> embedBatch(List<String> texts) throws Exception {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<float[]> allResults = new ArrayList<>();
        
        // Split into batches to respect API limits
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, texts.size()));
            System.out.println("🔄 Embedding batch " + (i / BATCH_SIZE + 1) + " (" + batch.size() + " texts)");
            allResults.addAll(embedBatchInternal(batch));
        }
        
        return allResults;
    }

    /**
     * Internal method to embed a single batch with retry logic.
     */
    private List<float[]> embedBatchInternal(List<String> texts) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return embedBatchOnce(texts);
            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    System.err.println("⚠️ Attempt " + attempt + " failed, retrying in " + RETRY_DELAY_MS + "ms: " + e.getMessage());
                    Thread.sleep(RETRY_DELAY_MS * attempt); // Exponential backoff
                }
            }
        }
        
        throw new Exception("Hugging Face API failed after " + MAX_RETRIES + " attempts", lastException);
    }

    /**
     * Single API call to embed texts.
     */
    private List<float[]> embedBatchOnce(List<String> texts) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Hugging Face API key is not configured");
        }

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + apiKey);

        // API endpoint - using new Inference Providers API
        String url = "https://router.huggingface.co/hf-inference/models/" + model;

        // For batch: send array of strings
        Map<String, Object> requestBody = Map.of(
            "inputs", texts.size() == 1 ? texts.get(0) : texts
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // Call API
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("API returned status: " + response.getStatusCode());
        }

        // Parse response
        JsonNode root = objectMapper.readTree(response.getBody());

        List<float[]> results = new ArrayList<>();

        // Handle single text response: [0.1, 0.2, ...]
        if (root.isArray() && !root.isEmpty() && root.get(0).isNumber()) {
            float[] embedding = parseEmbeddingArray(root);
            results.add(normalizeVector(embedding));
        }
        // Handle batch response: [[0.1, 0.2, ...], [0.3, 0.4, ...]]
        else if (root.isArray() && !root.isEmpty() && root.get(0).isArray()) {
            for (JsonNode embeddingNode : root) {
                float[] embedding = parseEmbeddingArray(embeddingNode);
                results.add(normalizeVector(embedding));
            }
        } else {
            throw new RuntimeException("Unexpected response format from Hugging Face API");
        }

        System.out.println("✅ Generated " + results.size() + " embeddings (dim=" + 
                          (results.isEmpty() ? 0 : results.get(0).length) + ")");

        return results;
    }

    /**
     * Parse JSON array to float[].
     */
    private float[] parseEmbeddingArray(JsonNode arrayNode) {
        float[] embedding = new float[arrayNode.size()];
        for (int i = 0; i < arrayNode.size(); i++) {
            embedding[i] = (float) arrayNode.get(i).asDouble();
        }
        return embedding;
    }

    /**
     * Normalize vector using L2 norm (for cosine similarity).
     * Essential for accurate cosine similarity calculations.
     */
    private float[] normalizeVector(float[] vector) {
        double sumSquares = 0.0;
        for (float v : vector) {
            sumSquares += v * v;
        }
        
        float norm = (float) Math.sqrt(Math.max(sumSquares, 1e-12)); // Avoid division by zero
        
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        
        return normalized;
    }

    /**
     * Get the model name being used.
     */
    public String getModel() {
        return this.model;
    }

    /**
     * Get embedding dimension for the model.
     * Returns 1024 for intfloat/multilingual-e5-large.
     * Returns 384 for sentence-transformers/all-MiniLM-L6-v2.
     */
    public int getDimension() {
        // Dimension depends on the model
        if (model.contains("multilingual-e5-large")) {
            return 1024;
        } else if (model.contains("all-MiniLM-L6-v2")) {
            return 384;
        }
        // Default fallback
        return 768;
    }
}
