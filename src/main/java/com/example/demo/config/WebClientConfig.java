package com.example.demo.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient Configuration with extended timeout for handling large API requests.
 * This configuration is applied globally to all WebClient instances used by Spring AI.
 * Timeout: 120 seconds for PDF processing and large context queries.
 * Authorization is handled by Spring AI OpenAiApi automatically.
 */
@Configuration
public class WebClientConfig {

    /**
     * Configure WebClient.Builder with extended timeout settings and OpenRouter headers.
     * - Connection timeout: 10 seconds
     * - Read timeout: 120 seconds (for large prompts and PDF processing)
     * - Write timeout: 120 seconds
     * - HTTP-Referer and X-Title for OpenRouter
     * NOTE: Authorization header is handled by Spring AI OpenAiApi automatically
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 10s connection timeout
                .responseTimeout(Duration.ofSeconds(120)) // 120s response timeout for large PDFs
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("HTTP-Referer", "http://localhost:1234")
                .defaultHeader("X-Title", "Spring AI RAG Demo");
    }
}

