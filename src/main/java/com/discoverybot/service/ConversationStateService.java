package com.discoverybot.service;

import com.discoverybot.dto.PendingUserDescription;

/**
 * Manages conversation state for users in the middle of multi-step flows.
 * Example: User starts /save, enricher asks for description, bot waits for their reply.
 */
public interface ConversationStateService {

    /**
     * Store a pending description for a user.
     * Automatically schedules a 5-minute timeout to clear the state if not completed.
     */
    void setState(Long userId, PendingUserDescription pending);

    /**
     * Get the pending state for a user, or null if they have no pending state.
     */
    PendingUserDescription getState(Long userId);

    /**
     * Clear a user's pending state (called when they complete the flow).
     */
    void clearState(Long userId);

    /**
     * Check if a user has a pending state.
     */
    boolean hasState(Long userId);
}
