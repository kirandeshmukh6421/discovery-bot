package com.discoverybot.dto;

import java.time.Instant;

/**
 * Holds the context for a user waiting to describe a discovery.
 * When an enricher returns askUser(), we store this state and wait for the user's reply.
 */
public record PendingUserDescription(
    String url,                // Original URL they tried to save (or raw input)
    String userNote,           // Original note they provided (can be null)
    Instant createdAt          // When the /save was initiated (for debugging/timeout)
) {}
