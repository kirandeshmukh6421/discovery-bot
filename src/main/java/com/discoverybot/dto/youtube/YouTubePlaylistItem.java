package com.discoverybot.dto.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YouTubePlaylistItem(
        YouTubeSnippet snippet,
        YouTubePlaylistContentDetails contentDetails
) {}
