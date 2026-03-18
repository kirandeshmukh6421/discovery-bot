package com.discoverybot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Structured data extracted by OpenRouter from a URL + text or plain text input.
 * All fields are nullable — the AI may not always provide every field.
 */
public record ExtractionResult(
        String category,
        String summary,
        List<String> tags,
        @JsonProperty("isPhysicalLocation") boolean isPhysicalLocation
) {}
