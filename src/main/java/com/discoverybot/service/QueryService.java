package com.discoverybot.service;

import com.discoverybot.model.Group;

public interface QueryService {

    String query(Group group, String userQuery);
}
