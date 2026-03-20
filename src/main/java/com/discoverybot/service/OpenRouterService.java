package com.discoverybot.service;

import com.discoverybot.dto.ExtractionResult;

public interface OpenRouterService {

    /**
     * Sends the given content (URL + text, or plain text) to the AI and returns
     * structured extraction data. Returns null if the call fails.
     */
    ExtractionResult extractDiscovery(String content);

    /**
     * Answers a user query conversationally based on the provided context string
     * of serialised discovery entries. Returns a plain-text reply.
     */
    String answerQuery(String context, String userQuery);
}
