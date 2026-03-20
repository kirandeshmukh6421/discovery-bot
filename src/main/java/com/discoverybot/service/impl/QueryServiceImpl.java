package com.discoverybot.service.impl;

import com.discoverybot.model.DiscoveryEntry;
import com.discoverybot.model.Group;
import com.discoverybot.repository.DiscoveryEntryRepository;
import com.discoverybot.service.DiscoveryEntryService;
import com.discoverybot.service.EmbeddingService;
import com.discoverybot.service.OpenRouterService;
import com.discoverybot.service.QueryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryServiceImpl implements QueryService {

    private static final int VECTOR_LIMIT = 15;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DiscoveryEntryService discoveryEntryService;
    private final DiscoveryEntryRepository discoveryEntryRepository;
    private final EmbeddingService embeddingService;
    private final OpenRouterService openRouterService;
    private final ObjectMapper objectMapper;

    @Override
    public String query(Group group, String userQuery) {
        List<DiscoveryEntry> entries = retrieveEntries(group, userQuery);

        if (entries.isEmpty()) {
            return "No discoveries saved in this group yet. Use /save to add some!";
        }

        log.info("Querying group {} with {} entries for: {}", group.getName(), entries.size(), userQuery);

        String context = buildContext(entries);
        return openRouterService.answerQuery(context, userQuery);
    }

    private List<DiscoveryEntry> retrieveEntries(Group group, String userQuery) {
        // Try vector search first
        float[] queryVector = embeddingService.embed(userQuery);
        if (queryVector != null) {
            List<DiscoveryEntry> similar = discoveryEntryRepository.findSimilar(
                    group.getId(),
                    DiscoveryEntryServiceImpl.toVectorString(queryVector),
                    VECTOR_LIMIT);
            if (!similar.isEmpty()) {
                log.info("Vector search returned {} entries for group {}", similar.size(), group.getName());
                return similar;
            }
            log.info("Vector search returned 0 results — falling back to recent 30");
        } else {
            log.info("Embedding failed — falling back to recent 30");
        }
        return discoveryEntryService.findRecent(group);
    }

    private String buildContext(List<DiscoveryEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            DiscoveryEntry e = entries.get(i);
            sb.append("[").append(i + 1).append("] ");
            sb.append("Category: ").append(e.getCategory() != null ? e.getCategory() : "unknown");
            sb.append(" | Source: ").append(e.getSource());
            sb.append(" | Tags: ").append(e.getTags() != null ? e.getTags() : "");
            sb.append("\n    ");

            String summary = extractSummary(e.getExtractedData());
            if (summary != null) {
                sb.append("Summary: ").append(summary).append("\n    ");
            }

            sb.append("Link: ").append(e.getRawInput() != null ? e.getRawInput() : "n/a");
            if (e.getUserNote() != null) {
                sb.append(" | Note: \"").append(e.getUserNote()).append("\"");
            }
            if (e.getCreatedAt() != null) {
                sb.append(" | Saved: ").append(e.getCreatedAt().format(DATE_FMT));
            }
            sb.append("\n\n");
        }
        return sb.toString();
    }

    private String extractSummary(String extractedDataJson) {
        if (extractedDataJson == null) return null;
        try {
            JsonNode node = objectMapper.readTree(extractedDataJson);
            JsonNode summaryNode = node.get("summary");
            if (summaryNode != null && !summaryNode.isNull()) {
                return summaryNode.asText();
            }
        } catch (Exception e) {
            log.debug("Could not parse extractedData for summary: {}", e.getMessage());
        }
        return null;
    }
}
