package com.discoverybot.dto.places;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlacesResult(
        String id,
        PlacesDisplayName displayName,
        String formattedAddress,
        Double rating,
        List<String> types,
        PlacesEditorialSummary editorialSummary
) {}
