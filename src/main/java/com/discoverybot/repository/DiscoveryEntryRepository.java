package com.discoverybot.repository;

import com.discoverybot.model.DiscoveryEntry;
import com.discoverybot.model.Group;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscoveryEntryRepository extends JpaRepository<DiscoveryEntry, Long> {

    List<DiscoveryEntry> findByGroupOrderByCreatedAtDesc(Group group, Pageable pageable);
}
