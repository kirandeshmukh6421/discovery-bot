package com.discoverybot.dto;

import java.time.Instant;

public record PendingUserDescription(
    String url,
    String userNote,
    Instant createdAt
) {}
