package com.discoverybot.service.impl;

import com.discoverybot.dto.PendingUserDescription;
import com.discoverybot.service.ConversationStateService;
import com.discoverybot.state.ConversationState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages conversation state for users waiting for replies.
 * Uses ConcurrentHashMap for thread-safe access (Telegram processes messages concurrently).
 * Uses ScheduledExecutorService to auto-clear expired states after 5 minutes.
 */
@Slf4j
@Service
public class ConversationStateServiceImpl implements ConversationStateService {

    private static final long TIMEOUT_MINUTES = 5;

    // Thread-safe map: userId → pending state
    private final Map<Long, PendingUserDescription> states = new ConcurrentHashMap<>();

    // Scheduler to auto-clear states after timeout
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void setState(Long userId, ConversationState state, PendingUserDescription pending) {
        states.put(userId, pending);
        log.info("User {} is waiting for description (state: {})", userId, state);

        // Schedule auto-cleanup after 5 minutes
        scheduler.schedule(() -> {
            if (states.containsKey(userId)) {
                states.remove(userId);
                log.info("User {} state timeout — cleared after {} minutes", userId, TIMEOUT_MINUTES);
            }
        }, TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public PendingUserDescription getState(Long userId) {
        return states.get(userId);
    }

    @Override
    public void clearState(Long userId) {
        if (states.remove(userId) != null) {
            log.info("User {} state cleared (they completed the flow)", userId);
        }
    }

    @Override
    public boolean hasState(Long userId) {
        return states.containsKey(userId);
    }
}
