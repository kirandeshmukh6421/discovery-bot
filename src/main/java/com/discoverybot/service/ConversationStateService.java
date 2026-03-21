package com.discoverybot.service;

import com.discoverybot.dto.PendingUserDescription;

public interface ConversationStateService {

    void setState(Long userId, PendingUserDescription pending);

    PendingUserDescription getState(Long userId);

    void clearState(Long userId);

    boolean hasState(Long userId);
}
