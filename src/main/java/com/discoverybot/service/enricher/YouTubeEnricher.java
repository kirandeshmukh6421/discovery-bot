package com.discoverybot.service.enricher;

import com.discoverybot.dto.EnrichmentResult;
import com.discoverybot.dto.ExtractionResult;
import com.discoverybot.dto.youtube.YouTubeApiResponse;
import com.discoverybot.dto.youtube.YouTubePlaylistItem;
import com.discoverybot.dto.youtube.YouTubePlaylistResponse;
import com.discoverybot.dto.youtube.YouTubeSnippet;
import com.discoverybot.dto.youtube.YouTubeVideoItem;
import com.discoverybot.model.Source;
import com.discoverybot.service.OpenRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class YouTubeEnricher {

    private static final String API_BASE = "https://www.googleapis.com/youtube/v3";

    private final WebClient webClient;
    private final String apiKey;
    private final OpenRouterService openRouterService;

    public YouTubeEnricher(WebClient.Builder webClientBuilder,
                           @Value("${youtube.api.key}") String apiKey,
                           OpenRouterService openRouterService) {
        this.webClient = webClientBuilder.baseUrl(API_BASE).build();
        this.apiKey = apiKey;
        this.openRouterService = openRouterService;
    }

    public Optional<EnrichmentResult> enrich(String url, String userNote) {
        if (isPlaylistUrl(url)) {
            String playlistId = extractPlaylistId(url);
            if (playlistId == null) {
                log.warn("Could not extract playlist ID from URL: {}", url);
                return Optional.empty();
            }
            log.info("Fetching YouTube playlist: {}", playlistId);
            return enrichPlaylist(url, playlistId, userNote);
        }

        String videoId = extractVideoId(url);
        if (videoId == null) {
            log.warn("Could not extract video ID from URL: {}", url);
            return Optional.empty();
        }
        log.info("Fetching YouTube video: {}", videoId);
        return enrichVideo(url, videoId, userNote);
    }

    private Optional<EnrichmentResult> enrichVideo(String url, String videoId, String userNote) {
        try {
            YouTubeApiResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/videos")
                            .queryParam("id", videoId)
                            .queryParam("part", "snippet,contentDetails,statistics")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(YouTubeApiResponse.class)
                    .block();

            if (response == null || response.items() == null || response.items().isEmpty()) {
                log.warn("YouTube API returned no results for video ID: {} — video may be private or deleted", videoId);
                return Optional.empty();
            }

            YouTubeVideoItem item = response.items().get(0);
            log.info("Fetched video: {} by {}", item.snippet().title(), item.snippet().channelTitle());

            ExtractionResult extraction = videoToExtractionResult(item, userNote);
            if (extraction == null) {
                log.error("OpenRouter extraction returned null for YouTube video: {}", url);
                return Optional.empty();
            }
            return Optional.of(EnrichmentResult.success(extraction, Source.YOUTUBE));

        } catch (Exception e) {
            log.error("YouTube video enrichment failed for URL {}: {}", url, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private Optional<EnrichmentResult> enrichPlaylist(String url, String playlistId, String userNote) {
        try {
            YouTubePlaylistResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/playlists")
                            .queryParam("id", playlistId)
                            .queryParam("part", "snippet,contentDetails")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(YouTubePlaylistResponse.class)
                    .block();

            if (response == null || response.items() == null || response.items().isEmpty()) {
                log.warn("YouTube API returned no results for playlist ID: {} — playlist may be private or deleted", playlistId);
                return Optional.empty();
            }

            YouTubePlaylistItem item = response.items().get(0);
            log.info("Fetched playlist: {} by {} ({} videos)",
                    item.snippet().title(), item.snippet().channelTitle(),
                    item.contentDetails() != null ? item.contentDetails().itemCount() : "?");

            ExtractionResult extraction = playlistToExtractionResult(item, userNote);
            if (extraction == null) {
                log.error("OpenRouter extraction returned null for YouTube playlist: {}", url);
                return Optional.empty();
            }
            return Optional.of(EnrichmentResult.success(extraction, Source.YOUTUBE));

        } catch (Exception e) {
            log.error("YouTube playlist enrichment failed for URL {}: {}", url, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private ExtractionResult videoToExtractionResult(YouTubeVideoItem item, String userNote) {
        YouTubeSnippet snippet = item.snippet();
        String content = buildVideoContent(snippet.title(), snippet.channelTitle(), snippet.description(), userNote);
        return withChannel(openRouterService.extractDiscovery(content), snippet.channelTitle());
    }

    private ExtractionResult playlistToExtractionResult(YouTubePlaylistItem item, String userNote) {
        YouTubeSnippet snippet = item.snippet();
        int count = item.contentDetails() != null ? item.contentDetails().itemCount() : 0;
        String content = buildVideoContent(
                snippet.title() + " (" + count + " videos)",
                snippet.channelTitle(),
                snippet.description(),
                userNote
        );
        return withChannel(openRouterService.extractDiscovery(content), snippet.channelTitle());
    }

    private ExtractionResult withChannel(ExtractionResult result, String channel) {
        if (result == null || channel == null) return result;
        List<String> tags = new ArrayList<>(result.tags() != null ? result.tags() : List.of());
        tags.add(channel.toLowerCase());
        return new ExtractionResult(result.category(), result.summary(), tags, result.isPhysicalLocation());
    }

    private String buildVideoContent(String title, String channel, String description, String userNote) {
        StringBuilder sb = new StringBuilder();
        if (title != null) sb.append("Title: ").append(title).append("\n");
        if (channel != null) sb.append("Channel: ").append(channel).append("\n");
        if (description != null && !description.isBlank()) {
            String trimmed = description.length() > 500 ? description.substring(0, 1000) + "..." : description;
            sb.append("Description: ").append(trimmed).append("\n");
        }
        if (userNote != null && !userNote.isBlank()) {
            sb.append("User note: ").append(userNote);
        }
        return sb.toString();
    }

    private boolean isPlaylistUrl(String url) {
        try {
            String path = URI.create(url).getPath();
            return path != null && path.equals("/playlist");
        } catch (Exception e) {
            return false;
        }
    }

    private String extractPlaylistId(String url) {
        try {
            String query = URI.create(url).getQuery();
            if (query == null) return null;
            for (String param : query.split("&")) {
                if (param.startsWith("list=")) {
                    return URLDecoder.decode(param.substring(5), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse playlist URL {}: {}", url, e.getMessage());
        }
        return null;
    }

    private String extractVideoId(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) return null;

            if (host.contains("youtu.be")) {
                String path = uri.getPath();
                return path != null && path.length() > 1 ? path.substring(1) : null;
            }

            if (host.contains("youtube.com")) {
                String path = uri.getPath();
                if (path != null && path.startsWith("/shorts/")) {
                    String id = path.substring("/shorts/".length());
                    return id.isEmpty() ? null : id;
                }
                String query = uri.getQuery();
                if (query == null) return null;
                for (String param : query.split("&")) {
                    if (param.startsWith("v=")) {
                        return URLDecoder.decode(param.substring(2), StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse YouTube URL {}: {}", url, e.getMessage());
        }
        return null;
    }
}
