package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Hugging Face Embedding Service
 * Uses Hugging Face Inference API for text embeddings
 */
@Service
public class HuggingFaceEmbeddingService {

    @Value("${huggingface.api.key}")
    private String apiKey;

    @Value("${huggingface.embedding.model}")
    private String model;

    @Value("${huggingface.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate embedding vector for given text
     * @param text Input text to embed
     * @return float array representing the embedding vector
     * @throws Exception if API call fails
     */
    public float[] embed(String text) throws Exception {
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // Prepare request body
            Map<String, Object> requestBody = Map.of(
                "inputs", text,
                "options", Map.of("wait_for_model", true)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Build URL with model
            String url = apiUrl + "/" + model;

            // Call Hugging Face API
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Hugging Face API returned status: " + response.getStatusCode());
            }

            // Parse response
            JsonNode root = objectMapper.readTree(response.getBody());

            // Handle different response formats
            if (root.isArray() && root.size() > 0) {
                JsonNode embeddingNode = root.get(0);
                
                // Convert to float array
                float[] embedding = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    embedding[i] = (float) embeddingNode.get(i).asDouble();
                }
                
                return embedding;
            } else {
                throw new RuntimeException("Unexpected response format from Hugging Face API");
            }

        } catch (Exception e) {
            throw new Exception("Hugging Face embedding failed: " + e.getMessage(), e);
        }
    }
}
