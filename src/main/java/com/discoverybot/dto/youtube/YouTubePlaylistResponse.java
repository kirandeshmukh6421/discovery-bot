package com.discoverybot.dto.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YouTubePlaylistResponse(
        List<YouTubePlaylistItem> items
) {}
