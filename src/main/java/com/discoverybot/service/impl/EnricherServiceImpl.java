package com.discoverybot.service.impl;

import com.discoverybot.dto.EnrichmentResult;
import com.discoverybot.service.EnricherService;
import com.discoverybot.service.enricher.YouTubeEnricher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates the enrichment chain for a URL.
 */
@Slf4j
@Service
public class EnricherServiceImpl implements EnricherService {

    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE);

    private final YouTubeEnricher youTubeEnricher;

    public EnricherServiceImpl(YouTubeEnricher youTubeEnricher) {
        this.youTubeEnricher = youTubeEnricher;
    }

    @Override
    public EnrichmentResult enrich(String url) {
        LinkType linkType = detectLinkType(url);
        log.info("Enrichment requested for {} link: {}", linkType, url);

        if (linkType == LinkType.YOUTUBE) {
            return youTubeEnricher.enrich(url)
                    .orElseGet(() -> {
                        log.debug("YouTube enricher returned empty for {} — requesting user description", url);
                        return EnrichmentResult.askUser();
                    });
        }

        // Phase 4: Google Places enricher — coming next
        // Phase 5: Open Graph enricher — coming next
        log.debug("No enricher matched for {} — requesting user description", url);
        return EnrichmentResult.askUser();
    }

    /**
     * Extracts the first URL from an arbitrary string, or returns null.
     */
    public static String extractUrl(String text) {
        if (text == null) return null;
        Matcher m = URL_PATTERN.matcher(text);
        return m.find() ? m.group() : null;
    }

    /**
     * Returns the text from {@code input} with the given {@code url} removed and trimmed.
     */
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
