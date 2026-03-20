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

    /** Returns the 30 most recently saved entries for the group. */
    List<DiscoveryEntry> findRecent(Group group);

    /** Returns the 10 most recently saved entries for the group (for /list). */
    List<DiscoveryEntry> listRecent(Group group);

    /** Deletes entry by id scoped to the group. Returns false if not found. */
    boolean delete(Long entryId, Group group);

    /** Deletes all entries for the group. */
    void reset(Group group);

    /** Persists a pre-computed embedding vector for the given entry. */
    void updateEmbedding(Long entryId, float[] vector);
}
