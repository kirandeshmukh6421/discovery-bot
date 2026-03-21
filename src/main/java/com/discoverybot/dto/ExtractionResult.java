package com.discoverybot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExtractionResult(
        String category,
        String summary,
        List<String> tags,
        @JsonProperty("isPhysicalLocation") boolean isPhysicalLocation
) {}
