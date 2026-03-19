package com.discoverybot.service.enricher;

import com.discoverybot.dto.EnrichmentResult;
import com.discoverybot.dto.ExtractionResult;
import com.discoverybot.dto.places.PlacesResult;
import com.discoverybot.dto.places.PlacesTextSearchResponse;
import com.discoverybot.model.Source;
import com.discoverybot.service.OpenRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GooglePlacesEnricher {

    private static final String API_BASE = "https://places.googleapis.com/v1";
    private static final String FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress,places.rating,places.types,places.editorialSummary";
    private static final Pattern COORDINATES_PATTERN =
            Pattern.compile("^-?\\d+\\.\\d+,\\s*-?\\d+\\.\\d+$");

    private final WebClient webClient;
    private final String apiKey;
    private final OpenRouterService openRouterService;

    public GooglePlacesEnricher(WebClient.Builder webClientBuilder,
                                @Value("${google.places.api.key}") String apiKey,
                                OpenRouterService openRouterService) {
        this.webClient = webClientBuilder.baseUrl(API_BASE).build();
        this.apiKey = apiKey;
        this.openRouterService = openRouterService;
    }

    public Optional<EnrichmentResult> enrich(String url, String userNote) {
        String resolved = resolveUrl(url);
        if (resolved == null) {
            log.warn("Could not resolve URL: {}", url);
            return Optional.empty();
        }

        try {
            PlacesResult place = findPlace(resolved);
            if (place == null) {
                log.warn("Places search returned no results for resolved URL: {}", resolved);
                return Optional.empty();
            }

            String name = place.displayName() != null ? place.displayName().text() : null;
            log.info("Found place: {} ({})", name, place.id());

            ExtractionResult extraction = openRouterService.extractDiscovery(buildContent(place, userNote));
            if (extraction == null) {
                log.error("OpenRouter extraction returned null for place: {}", name);
                return Optional.empty();
            }

            return Optional.of(EnrichmentResult.success(extraction, Source.GOOGLE_PLACES));

        } catch (Exception e) {
            log.error("Google Places enrichment failed for URL {}: {}", url, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Routes to the correct Places API call based on what the resolved URL contains.
     * - Coordinates (pin drop) → searchNearby
     * - Place name → searchText
     */
    private PlacesResult findPlace(String resolvedUrl) {
        // Check for coordinates in q= param first
        double[] coords = extractCoordinates(resolvedUrl);
        if (coords != null) {
            log.info("Detected pin drop coordinates: {}, {} — using nearby search", coords[0], coords[1]);
            return nearbySearch(coords[0], coords[1]);
        }

        // Fall back to place name text search
        String placeName = extractPlaceName(resolvedUrl);
        if (placeName == null) {
            log.warn("Could not extract place name or coordinates from URL: {}", resolvedUrl);
            return null;
        }
        log.info("Searching Places API for: {}", placeName);
        return textSearch(placeName);
    }

    private PlacesResult textSearch(String query) {
        PlacesTextSearchResponse response = webClient.post()
                .uri("/places:searchText")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .bodyValue(Map.of("textQuery", query))
                .retrieve()
                .bodyToMono(PlacesTextSearchResponse.class)
                .block();

        if (response == null || response.places() == null || response.places().isEmpty()) {
            return null;
        }
        return response.places().get(0);
    }

    private PlacesResult nearbySearch(double lat, double lng) {
        Map<String, Object> body = Map.of(
                "locationRestriction", Map.of(
                        "circle", Map.of(
                                "center", Map.of("latitude", lat, "longitude", lng),
                                "radius", 50.0
                        )
                )
        );

        PlacesTextSearchResponse response = webClient.post()
                .uri("/places:searchNearby")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(PlacesTextSearchResponse.class)
                .block();

        if (response == null || response.places() == null || response.places().isEmpty()) {
            return null;
        }
        return response.places().get(0);
    }

    private String buildContent(PlacesResult place, String userNote) {
        StringBuilder sb = new StringBuilder();
        if (place.displayName() != null) sb.append("Name: ").append(place.displayName().text()).append("\n");
        if (place.formattedAddress() != null) sb.append("Address: ").append(place.formattedAddress()).append("\n");
        if (place.rating() != null) sb.append("Rating: ").append(place.rating()).append("/5\n");
        if (place.types() != null && !place.types().isEmpty()) {
            sb.append("Types: ").append(String.join(", ", place.types())).append("\n");
        }
        if (place.editorialSummary() != null && place.editorialSummary().text() != null) {
            sb.append("About: ").append(place.editorialSummary().text()).append("\n");
        }
        if (userNote != null && !userNote.isBlank()) {
            sb.append("User note: ").append(userNote);
        }
        return sb.toString();
    }

    /**
     * Returns [lat, lng] if q= param contains coordinates, null otherwise.
     */
    private double[] extractCoordinates(String url) {
        try {
            String query = URI.create(url).getQuery();
            if (query == null) return null;
            for (String param : query.split("&")) {
                if (param.startsWith("q=")) {
                    String value = URLDecoder.decode(param.substring(2), StandardCharsets.UTF_8).trim();
                    if (COORDINATES_PATTERN.matcher(value).matches()) {
                        String[] parts = value.split(",");
                        return new double[]{
                                Double.parseDouble(parts[0].trim()),
                                Double.parseDouble(parts[1].trim())
                        };
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract coordinates from URL {}: {}", url, e.getMessage());
        }
        return null;
    }

    /**
     * Extracts a searchable place name from a resolved Google Maps URL.
     * Handles /maps/place/{Name}/ path and ?q= query param with address strings.
     */
    private String extractPlaceName(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();

            if (path != null && path.contains("/place/")) {
                String[] parts = path.split("/place/");
                if (parts.length > 1) {
                    String name = parts[1].split("/")[0];
                    return URLDecoder.decode(name.replace("+", " "), StandardCharsets.UTF_8);
                }
            }

            String query = uri.getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("q=")) {
                        String full = URLDecoder.decode(param.substring(2), StandardCharsets.UTF_8);
                        // Full address like "Place Name, Area, Street, City..." — keep only name + area
                        String[] parts = full.split(",");
                        return parts.length >= 2
                                ? parts[0].trim() + ", " + parts[1].trim()
                                : parts[0].trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Maps URL {}: {}", url, e.getMessage());
        }
        return null;
    }

    /**
     * Follows HTTP redirects to get the final URL.
     * Uses GET + reads the stream to ensure redirects are fully followed.
     */
    private String resolveUrl(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.getInputStream().close();
            String finalUrl = conn.getURL().toString();
            conn.disconnect();
            log.info("Resolved {} → {}", url, finalUrl);
            return finalUrl;
        } catch (Exception e) {
            log.error("Failed to resolve URL {}: {}", url, e.getMessage());
            return null;
        }
    }
}
