package com.discoverybot.service;

import com.discoverybot.dto.ExtractionResult;

public interface OpenRouterService {

    ExtractionResult extractDiscovery(String content);

    String answerQuery(String context, String userQuery);
}
