package com.discoverybot.dto;

import com.discoverybot.model.Source;

/**
 * Result of the enrichment chain.
 *
 * Three states:
 * - success: enrichment worked, extractionResult is populated
 * - askUser: no enricher available, bot should ask for a description (Phase 6)
 * - failed: enricher encountered an error, user is informed and asked to describe
 */
public record EnrichmentResult(
        boolean needsUserDescription,
        ExtractionResult extractionResult,
        Source source,
        String failureReason
) {

    public static EnrichmentResult askUser() {
        return new EnrichmentResult(true, null, null, null);
    }

    public static EnrichmentResult failed(String reason) {
        return new EnrichmentResult(true, null, null, reason);
    }

    public static EnrichmentResult success(ExtractionResult result, Source source) {
        return new EnrichmentResult(false, result, source, null);
    }

    public boolean isFailed() {
        return needsUserDescription && failureReason != null;
    }
}
