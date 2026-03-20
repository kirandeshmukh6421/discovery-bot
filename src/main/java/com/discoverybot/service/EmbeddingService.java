package com.discoverybot.service;

public interface EmbeddingService {

    /**
     * Generates a 1536-dimensional embedding vector for the given text.
     * Returns null if the API call fails.
     */
    float[] embed(String text);
}
