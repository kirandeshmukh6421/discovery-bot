package com.discoverybot.dto.places;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlacesEditorialSummary(
        String text
) {}
