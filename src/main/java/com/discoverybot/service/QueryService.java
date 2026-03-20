package com.discoverybot.service;

import com.discoverybot.model.Group;

public interface QueryService {

    /**
     * Answers a natural-language query against the saved discoveries for the group.
     */
    String query(Group group, String userQuery);
}
