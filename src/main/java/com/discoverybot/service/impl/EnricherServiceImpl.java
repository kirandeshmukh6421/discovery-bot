package com.discoverybot.service.impl;

import com.discoverybot.dto.EnrichmentResult;
import com.discoverybot.service.EnricherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates the enrichment chain for a URL.
 *
 * Phase 3: detects link type and logs which enricher would handle it.
 *          All specific enrichers (Phase 4) and Open Graph (Phase 5) are stubs.
 *          Falls through to asking the user for a description.
 */
@Slf4j
@Service
public class EnricherServiceImpl implements EnricherService {

    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE);

    @Override
    public EnrichmentResult enrich(String url) {
        LinkType linkType = detectLinkType(url);
        log.info("Enrichment requested for {} link: {}", linkType, url);

        // Phase 4: specific enrichers (Google Places, YouTube, Spotify) — stubs, fall through
        // Phase 5: Open Graph enricher — stub, fall through
        // Fall through to user description
        log.debug("All enrichers returned null for {} — requesting user description", url);
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
        if (url.contains("spotify.com")) {
            return LinkType.SPOTIFY;
        }
        return LinkType.GENERIC_URL;
    }

    public enum LinkType {
        GOOGLE_MAPS, YOUTUBE, SPOTIFY, GENERIC_URL
    }
}
