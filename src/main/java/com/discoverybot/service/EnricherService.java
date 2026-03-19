package com.discoverybot.service;

import com.discoverybot.dto.EnrichmentResult;

public interface EnricherService {

    EnrichmentResult enrich(String url, String userNote);

    default EnrichmentResult enrich(String url) {
        return enrich(url, null);
    }
}
