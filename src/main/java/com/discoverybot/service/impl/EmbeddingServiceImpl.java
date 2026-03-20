package com.discoverybot.service.impl;

import com.discoverybot.dto.embedding.EmbeddingRequest;
import com.discoverybot.dto.embedding.EmbeddingResponse;
import com.discoverybot.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final WebClient webClient;
    private final String model;

    public EmbeddingServiceImpl(
            WebClient.Builder webClientBuilder,
            @Value("${openrouter.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${openrouter.embedding.model}") String model) {
        this.model = model;
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public float[] embed(String text) {
        try {
            EmbeddingResponse response = webClient.post()
                    .uri("/v1/embeddings")
                    .bodyValue(new EmbeddingRequest(model, text))
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .block();

            if (response == null || response.data() == null || response.data().isEmpty()) {
                log.warn("Embedding API returned empty response for input: {}", text.substring(0, Math.min(50, text.length())));
                return null;
            }

            List<Float> embedding = response.data().get(0).embedding();
            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i);
            }
            return vector;

        } catch (Exception e) {
            log.error("Embedding API call failed: {}", e.getMessage());
            return null;
        }
    }
}
