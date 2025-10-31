package com.example.demo.service;

public interface AIClient {
    /**
     * Create embedding vector for given text.
     */
    float[] embed(String text) throws Exception;

    /**
     * Generate chat/response text for given prompt.
     */
    String chat(String prompt) throws Exception;
    
    /**
     * Generate chat/response text with custom system prompt.
     * @param systemPrompt Custom system prompt to override default
     * @param userPrompt User's question/prompt
     */
    String chatWithSystem(String systemPrompt, String userPrompt) throws Exception;
}
