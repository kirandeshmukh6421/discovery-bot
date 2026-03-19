package com.discoverybot.dto.places;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlacesTextSearchResponse(
        List<PlacesResult> places
) {}
