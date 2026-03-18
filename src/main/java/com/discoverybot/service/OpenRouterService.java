package com.discoverybot.service;

import com.discoverybot.dto.ExtractionResult;

public interface OpenRouterService {

    /**
     * Sends the given content (URL + text, or plain text) to the AI and returns
     * structured extraction data. Returns null if the call fails.
     */
    ExtractionResult extractDiscovery(String content);
}
