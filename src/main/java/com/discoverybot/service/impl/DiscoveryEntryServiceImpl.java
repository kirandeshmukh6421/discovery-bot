package com.discoverybot.service.impl;

import com.discoverybot.dto.ExtractionResult;
import com.discoverybot.model.DiscoveryEntry;
import com.discoverybot.model.Group;
import com.discoverybot.model.Source;
import com.discoverybot.model.User;
import com.discoverybot.repository.DiscoveryEntryRepository;
import com.discoverybot.service.DiscoveryEntryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoveryEntryServiceImpl implements DiscoveryEntryService {

    private final DiscoveryEntryRepository repository;
    private final ObjectMapper objectMapper;

    @Override
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
        return saved;
    }

    @Override
    public List<DiscoveryEntry> findRecent(Group group, int limit) {
        return repository.findByGroupOrderByCreatedAtDesc(group, PageRequest.of(0, limit));
    }
}
