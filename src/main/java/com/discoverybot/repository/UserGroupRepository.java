package com.discoverybot.repository;

import com.discoverybot.model.Group;
import com.discoverybot.model.User;
import com.discoverybot.model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    Optional<UserGroup> findByUserAndGroup(User user, Group group);
    boolean existsByGroup(Group group);
}
