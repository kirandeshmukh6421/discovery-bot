package com.discoverybot.service.impl;

import com.discoverybot.dto.EnrichmentResult;
import com.discoverybot.service.EnricherService;
import com.discoverybot.service.enricher.GooglePlacesEnricher;
import com.discoverybot.service.enricher.OpenGraphEnricher;
import com.discoverybot.service.enricher.YouTubeEnricher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EnricherServiceImpl implements EnricherService {

    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE);

    private final YouTubeEnricher youTubeEnricher;
    private final GooglePlacesEnricher googlePlacesEnricher;
    private final OpenGraphEnricher openGraphEnricher;

    public EnricherServiceImpl(YouTubeEnricher youTubeEnricher,
                               GooglePlacesEnricher googlePlacesEnricher,
                               OpenGraphEnricher openGraphEnricher) {
        this.youTubeEnricher = youTubeEnricher;
        this.googlePlacesEnricher = googlePlacesEnricher;
        this.openGraphEnricher = openGraphEnricher;
    }

    @Override
    public EnrichmentResult enrich(String url, String userNote) {
        LinkType linkType = detectLinkType(url);
        log.info("Enrichment requested for {} link: {}", linkType, url);

        if (linkType == LinkType.YOUTUBE) {
            return youTubeEnricher.enrich(url, userNote)
                    .orElseGet(() -> {
                        log.warn("YouTube enricher failed for {}", url);
                        return EnrichmentResult.failed("Couldn't fetch details from YouTube");
                    });
        }

        if (linkType == LinkType.GOOGLE_MAPS) {
            return googlePlacesEnricher.enrich(url, userNote)
                    .orElseGet(() -> {
                        log.warn("Google Places enricher failed for {}", url);
                        return EnrichmentResult.failed("Couldn't fetch details from Google Maps");
                    });
        }

        return openGraphEnricher.enrich(url, userNote)
                .orElseGet(() -> {
                    log.info("Open Graph enricher returned empty for {} — asking user for description", url);
                    return EnrichmentResult.askUser();
                });
    }

    public static String extractUrl(String text) {
        if (text == null) return null;
        Matcher m = URL_PATTERN.matcher(text);
        return m.find() ? m.group() : null;
    }

    public static String textWithoutUrl(String input, String url) {
        return input.replace(url, "").trim();
    }

    private LinkType detectLinkType(String url) {
        if (url.contains("maps.google.com") || url.contains("goo.gl/maps")
                || url.contains("maps.app.goo.gl")) {
            return LinkType.GOOGLE_MAPS;
        }
        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            return LinkType.YOUTUBE;
        }
        return LinkType.GENERIC_URL;
    }

    public enum LinkType {
        GOOGLE_MAPS, YOUTUBE, GENERIC_URL
    }
}
