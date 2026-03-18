package com.discoverybot.dto;

import com.discoverybot.model.Source;

/**
 * Result of the enrichment chain.
 * If {@code needsUserDescription} is true, the bot should ask the user for more context
 * and wait for their reply (handled in Phase 6). extractionResult will be null in that case.
 */
public record EnrichmentResult(
        boolean needsUserDescription,
        ExtractionResult extractionResult,
        Source source
) {

    public static EnrichmentResult askUser() {
        return new EnrichmentResult(true, null, null);
    }

    public static EnrichmentResult success(ExtractionResult result, Source source) {
        return new EnrichmentResult(false, result, source);
    }
}
