package com.discoverybot.dto;

import com.discoverybot.model.Source;

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
