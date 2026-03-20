package com.discoverybot.repository;

import com.discoverybot.model.DiscoveryEntry;
import com.discoverybot.model.Group;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DiscoveryEntryRepository extends JpaRepository<DiscoveryEntry, Long> {

    List<DiscoveryEntry> findByGroupOrderByCreatedAtDesc(Group group, Pageable pageable);

    @Modifying
    @Query(value = "UPDATE discovery_entries SET embedding = CAST(:embedding AS vector) WHERE id = :id",
           nativeQuery = true)
    void updateEmbedding(@Param("id") Long id, @Param("embedding") String embedding);

    @Query(value = """
            SELECT * FROM discovery_entries
            WHERE group_id = :groupId
              AND embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<DiscoveryEntry> findSimilar(@Param("groupId") Long groupId,
                                     @Param("embedding") String embedding,
                                     @Param("limit") int limit);

    @Modifying
    @Query("DELETE FROM DiscoveryEntry e WHERE e.id = :id AND e.group = :group")
    int deleteByIdAndGroup(@Param("id") Long id, @Param("group") Group group);

    @Modifying
    @Query("DELETE FROM DiscoveryEntry e WHERE e.group = :group")
    void deleteAllByGroup(@Param("group") Group group);
}
