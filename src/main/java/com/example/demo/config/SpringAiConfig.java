package com.example.demo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Spring AI Configuration for OpenRouter integration.
 * Configures OpenAI-compatible API client for CHAT ONLY with proper timeout.
 * Embedding is handled by Hugging Face service.
 */
@Configuration
public class SpringAiConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://openrouter.ai/api}")
    private String baseUrl;

    /**
     * Configure HTTP Request Factory with 120s timeout.
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(120));
        return factory;
    }

    /**
     * Configure RestClient.Builder with timeout and OpenRouter headers.
     */
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder(ClientHttpRequestFactory requestFactory) {
        System.out.println("🔧 Configuring RestClient.Builder with:");
        System.out.println("   Timeout: 120 seconds");
        System.out.println("   OpenRouter headers included");
        
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("HTTP-Referer", "http://localhost:1234")
                .defaultHeader("X-Title", "Spring AI RAG Demo");
    }

    /**
     * Configure OpenAiApi with OpenRouter base URL and custom timeout settings.
     * Uses both RestClient.Builder and WebClient.Builder with 120s timeout.
     */
    @Bean
    @Primary
    public OpenAiApi openAiApi(RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder) {
        System.out.println("🔑 Configuring OpenAiApi with:");
        System.out.println("   Base URL: " + baseUrl);
        System.out.println("   API Key: " + (apiKey != null && !apiKey.isEmpty() ? 
            apiKey.substring(0, Math.min(20, apiKey.length())) + "..." : "NOT SET"));
        
        return new OpenAiApi(baseUrl, apiKey, restClientBuilder, webClientBuilder);
    }

    /**
     * Configure ChatModel for OpenRouter.
     */
    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        return new OpenAiChatModel(openAiApi);
    }

    /**
     * Configure ChatClient for convenient chat operations.
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}

