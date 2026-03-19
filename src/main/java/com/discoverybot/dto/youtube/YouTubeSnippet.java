package com.discoverybot.dto.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YouTubeSnippet(
        String title,
        String description,
        String channelTitle,
        List<String> tags
) {}
