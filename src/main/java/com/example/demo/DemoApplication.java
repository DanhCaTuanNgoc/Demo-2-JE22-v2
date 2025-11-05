package com.example.demo;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application
 * Excludes OpenAiAutoConfiguration to prevent auto-config of unnecessary beans
 * (image, audio, moderation, etc.). We manually configure only Chat via SpringAiConfig.
 */
@SpringBootApplication(exclude = {OpenAiAutoConfiguration.class})
public class DemoApplication {
    public static void main(String[] args) {
        // Load .env file before starting Spring Boot application
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            
            // Set environment variables from .env file
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
                System.out.println("✅ Loaded from .env: " + entry.getKey());
            });
            
            System.out.println("🔧 .env file loaded successfully!");
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not load .env file - " + e.getMessage());
        }
        
        SpringApplication.run(DemoApplication.class, args);
    }
}
