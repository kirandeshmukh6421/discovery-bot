package com.discoverybot.service.enricher;

import com.discoverybot.dto.EnrichmentResult;
import com.discoverybot.dto.ExtractionResult;
import com.discoverybot.model.Source;
import com.discoverybot.service.OpenRouterService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class OpenGraphEnricher {

    private final OpenRouterService openRouterService;

    public OpenGraphEnricher(OpenRouterService openRouterService) {
        this.openRouterService = openRouterService;
    }

    public Optional<EnrichmentResult> enrich(String url, String userNote) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; DiscoveryBot/1.0)")
                    .timeout(5000)
                    .get();

            String title = ogContent(doc, "og:title");
            String description = ogContent(doc, "og:description");
            String siteName = ogContent(doc, "og:site_name");

            if (isUseless(title, description)) {
                log.info("Open Graph data unusable for {} (title={}, description={})", url, title, description);
                return Optional.empty();
            }

            log.info("Open Graph scraped: title='{}', site='{}'", title, siteName);

            ExtractionResult extraction = openRouterService.extractDiscovery(buildContent(title, description, siteName, url, userNote));
            if (extraction == null) {
                log.error("OpenRouter extraction returned null for OG data from {}", url);
                return Optional.empty();
            }

            return Optional.of(EnrichmentResult.success(extraction, Source.OPEN_GRAPH));

        } catch (Exception e) {
            log.warn("Open Graph enrichment failed for {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private String ogContent(Document doc, String property) {
        return doc.select("meta[property=" + property + "]").attr("content").trim();
    }

    private boolean isUseless(String title, String description) {
        return (title == null || title.isBlank()) && (description == null || description.isBlank());
    }

    private String buildContent(String title, String description, String siteName, String url, String userNote) {
        StringBuilder sb = new StringBuilder();
        if (siteName != null && !siteName.isBlank()) sb.append("Site: ").append(siteName).append("\n");
        if (title != null && !title.isBlank()) sb.append("Title: ").append(title).append("\n");
        if (description != null && !description.isBlank()) sb.append("Description: ").append(description).append("\n");
        sb.append("URL: ").append(url).append("\n");
        if (userNote != null && !userNote.isBlank()) sb.append("User note: ").append(userNote);
        return sb.toString();
    }
}
