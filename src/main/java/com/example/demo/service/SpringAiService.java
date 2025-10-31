package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Hybrid AI implementation: OpenRouter for Chat + Hugging Face for Embedding
 */
@Service
public class SpringAiService implements AIClient {

    private final ChatClient chatClient;
    private final HuggingFaceEmbeddingService huggingFaceEmbeddingService;
    
    @Value("${spring.ai.openai.chat.options.model:meta-llama/llama-3.1-70b-instruct}")
    private String chatModel;
    
    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;
    
    @Value("${spring.ai.openai.chat.options.max-tokens:1000}")
    private Integer maxTokens;
    
    @Value("${ai.system.prompt:You are a helpful AI assistant.}")
    private String systemPrompt;

    public SpringAiService(ChatClient chatClient, HuggingFaceEmbeddingService huggingFaceEmbeddingService) {
        this.chatClient = chatClient;
        this.huggingFaceEmbeddingService = huggingFaceEmbeddingService;
        System.out.println("✅ SpringAiService initialized with OpenRouter (Chat) + Hugging Face (Embedding)");
    }

    @Override
    public float[] embed(String text) throws Exception {
        try {
            // Use Hugging Face for embedding
            return huggingFaceEmbeddingService.embed(text);
        } catch (Exception e) {
            throw new Exception("Hugging Face embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String chat(String prompt) throws Exception {
        try {
            // Create chat options for OpenRouter
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel(chatModel)
                    .withTemperature(temperature)
                    .withMaxTokens(maxTokens)
                    .build();
            
            // Use ChatClient to send prompt with system message
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(prompt)
                    .options(options)
                    .call()
                    .content();
            
            return response != null ? response : "";
        } catch (Exception e) {
            throw new Exception("Spring AI chat failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String chatWithSystem(String customSystemPrompt, String userPrompt) throws Exception {
        try {
            // Create chat options for OpenRouter
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel(chatModel)
                    .withTemperature(temperature)
                    .withMaxTokens(maxTokens)
                    .build();
            
            // Use ChatClient to send prompt with custom system message
            String response = chatClient.prompt()
                    .system(customSystemPrompt)
                    .user(userPrompt)
                    .options(options)
                    .call()
                    .content();
            
            return response != null ? response : "";
        } catch (Exception e) {
            throw new Exception("Spring AI chat with custom system failed: " + e.getMessage(), e);
        }
    }
}
