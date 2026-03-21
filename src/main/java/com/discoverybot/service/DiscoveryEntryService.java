package com.discoverybot.service;

import com.discoverybot.dto.ExtractionResult;
import com.discoverybot.model.DiscoveryEntry;
import com.discoverybot.model.Group;
import com.discoverybot.model.Source;
import com.discoverybot.model.User;

import java.util.List;

public interface DiscoveryEntryService {

    DiscoveryEntry save(User user, Group group, String rawInput, String userNote,
                        ExtractionResult extraction, Source source);

    List<DiscoveryEntry> findRecent(Group group);

    List<DiscoveryEntry> listRecent(Group group);

    boolean delete(Long entryId, Group group);

    void reset(Group group);

    void updateEmbedding(Long entryId, float[] vector);
}
