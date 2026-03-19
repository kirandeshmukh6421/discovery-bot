package com.discoverybot.service.impl;

import com.discoverybot.dto.ExtractionResult;
import com.discoverybot.service.OpenRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpenRouterServiceImpl implements OpenRouterService {

    private static final String SYSTEM_PROMPT = """
            You are a discovery extraction assistant.

            Given any content, extract whatever information you can and return
            ONLY a raw JSON object. No explanation. No markdown. No preamble. Just JSON.

            Fields:
            - category: one or two word lowercase label describing what this is.
                        Be as specific as possible. Examples: "restaurant", "cafe", "temple",
                        "park", "hotel", "bar", "museum", "beach", "trek", "waterfall",
                        "book", "music", "article", "movie", "podcast", "product", "fitness",
                        "video", "playlist" — or anything else that fits. Avoid generic labels
                        like "place" or "location" — always prefer the specific type.
            - summary: 1-2 sentence human friendly description. If a place, business, or
                        creator name is mentioned, always include it in the summary. null if cannot determine.
            - tags: array of 4-8 lowercase tags covering the key aspects: what it is, where it is,
                        who made it, relevant descriptors. Enough to make it easily queryable.
                        Empty array if cannot determine.
            - isPhysicalLocation: true only if this is a place you can physically visit.
                                  false for everything else.

            Rules:
            - Extract only what is clearly present in the input. Do not guess or invent.
            - If a field cannot be determined use null (or [] for tags).
            - Always return valid JSON. Nothing else.
            - Ignore promotional noise: merch links, subscribe reminders, other channel plugs,
              affiliate links, sponsor segments, or any self-promotion in descriptions.
              Focus only on what the content is actually about.

            Example:
            Input: "Amazing wood fired pizza in Indiranagar, must visit"
            Output:
            {
              "category": "food",
              "summary": "A highly recommended wood fired pizza place in Indiranagar.",
              "tags": ["pizza", "wood fired", "indiranagar", "dine out"],
              "isPhysicalLocation": true
            }

            Example:
            Input: "Atomic Habits by James Clear"
            Output:
            {
              "category": "book",
              "summary": "Atomic Habits is a popular self-improvement book by James Clear.",
              "tags": ["habits", "self improvement", "productivity", "james clear"],
              "isPhysicalLocation": false
            }

            Example:
            Input: "https://instagram.com/reel/abc123"
            Output:
            {
              "category": "other",
              "summary": null,
              "tags": [],
              "isPhysicalLocation": false
            }
            """;

    private final ChatClient chatClient;

    public OpenRouterServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @Override
    public ExtractionResult extractDiscovery(String content) {
        try {
            return chatClient.prompt()
                    .user(content)
                    .call()
                    .entity(ExtractionResult.class);
        } catch (Exception e) {
            log.error("OpenRouter extraction failed: {}", e.getMessage());
            return null;
        }
    }
}
