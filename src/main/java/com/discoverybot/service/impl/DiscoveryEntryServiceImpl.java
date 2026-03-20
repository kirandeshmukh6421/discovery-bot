package com.discoverybot.service.impl;

import com.discoverybot.dto.ExtractionResult;
import com.discoverybot.model.DiscoveryEntry;
import com.discoverybot.model.Group;
import com.discoverybot.model.Source;
import com.discoverybot.model.User;
import com.discoverybot.repository.DiscoveryEntryRepository;
import com.discoverybot.service.DiscoveryEntryService;
import com.discoverybot.service.EmbeddingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoveryEntryServiceImpl implements DiscoveryEntryService {

    private final DiscoveryEntryRepository repository;
    private final ObjectMapper objectMapper;
    private final EmbeddingService embeddingService;

    @Override
    @Transactional
    public DiscoveryEntry save(User user, Group group, String rawInput, String userNote,
                               ExtractionResult extraction, Source source) {
        DiscoveryEntry entry = new DiscoveryEntry();
        entry.setGroup(group);
        entry.setAddedBy(user);
        entry.setRawInput(rawInput);
        entry.setUserNote(userNote);
        entry.setSource(source);

        if (extraction != null) {
            entry.setCategory(extraction.category());
            if (extraction.tags() != null && !extraction.tags().isEmpty()) {
                entry.setTags(String.join(",", extraction.tags()));
            }
            try {
                entry.setExtractedData(objectMapper.writeValueAsString(extraction));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialise extraction result for entry: {}", e.getMessage());
            }
        }

        DiscoveryEntry saved = repository.save(entry);
        log.info("Saved discovery entry {} for group {} by {}", saved.getId(), group.getName(), user.getName());

        // Generate and store embedding asynchronously (best-effort)
        if (extraction != null) {
            String embeddingInput = buildEmbeddingInput(extraction, rawInput);
            float[] vector = embeddingService.embed(embeddingInput);
            if (vector != null) {
                updateEmbedding(saved.getId(), vector);
            }
        }

        return saved;
    }

    @Override
    public List<DiscoveryEntry> findRecent(Group group) {
        return repository.findByGroupOrderByCreatedAtDesc(group, PageRequest.of(0, 30));
    }

    @Override
    @Transactional
    public void updateEmbedding(Long entryId, float[] vector) {
        try {
            repository.updateEmbedding(entryId, toVectorString(vector));
        } catch (Exception e) {
            log.error("Failed to store embedding for entry {}: {}", entryId, e.getMessage());
        }
    }

    private String buildEmbeddingInput(ExtractionResult extraction, String rawInput) {
        StringBuilder sb = new StringBuilder();
        if (extraction.category() != null) 
            sb.append(extraction.category());
        if (extraction.tags() != null && !extraction.tags().isEmpty()) {
            sb.append(" | ").append(String.join(", ", extraction.tags()));
        }
        if (extraction.summary() != null) {
            sb.append(" | ").append(extraction.summary());
        } else if (rawInput != null) {
            sb.append(" | ").append(rawInput);
        }
        return sb.toString();
    }

    static String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) 
                sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
