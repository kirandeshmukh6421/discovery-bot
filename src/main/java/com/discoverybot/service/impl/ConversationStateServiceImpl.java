package com.discoverybot.service.impl;

import com.discoverybot.dto.PendingUserDescription;
import com.discoverybot.service.ConversationStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ConversationStateServiceImpl implements ConversationStateService {

    private static final long TIMEOUT_MINUTES = 5;

    private final Map<Long, PendingUserDescription> states = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void setState(Long userId, PendingUserDescription pending) {
        states.put(userId, pending);
        log.info("User {} is now waiting for description", userId);

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
