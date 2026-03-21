package com.discoverybot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * General application-level Spring beans.
 *
 * WebClient.Builder is declared here as a shared bean so all services
 * (enrichers, OpenRouterService, etc.) can inject it and build their own
 * configured WebClient instances without creating new builders per class.
 *
 * Per Spring's recommendation, inject WebClient.Builder (not WebClient directly)
 * so each consumer can apply its own base URL, headers, or filters.
 */
@EnableAsync
@Configuration
public class AppConfig {

    @Bean
    WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
