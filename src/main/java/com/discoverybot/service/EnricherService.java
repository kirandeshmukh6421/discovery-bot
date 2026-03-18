package com.discoverybot.service;

import com.discoverybot.dto.EnrichmentResult;

public interface EnricherService {

    /**
     * Attempts to enrich a URL by routing through the enrichment chain:
     * specific API enrichers (Phase 4) → Open Graph (Phase 5) → user description fallback.
     * <p>
     * Returns an {@link EnrichmentResult} indicating whether enrichment succeeded or
     * whether the bot should ask the user for a description.
     */
    EnrichmentResult enrich(String url);
}
