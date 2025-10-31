package com.example.demo;

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
        SpringApplication.run(DemoApplication.class, args);
    }
}
