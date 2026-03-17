package com.discoverybot.service.impl;

import com.discoverybot.model.User;
import com.discoverybot.repository.UserRepository;
import com.discoverybot.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handles user identity resolution using Telegram's user object.
 *
 * There is no login flow — the Telegram user ID is the identity.
 * Every incoming message triggers getOrCreate(), which either returns
 * the existing DB record or inserts a new one on first contact.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UserServiceImpl implements UserService {

    UserRepository userRepository;

    /**
     * Returns the existing user for the given Telegram user, or creates one if first contact.
     *
     * @param telegramUser The Telegram user object from the incoming update
     * @return Persisted User entity
     */
    @Override
    public User getOrCreate(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        // Lookup by Telegram's numeric user ID (stable, unique, never changes)
        return userRepository.findByTelegramId(telegramUser.getId())
                .orElseGet(() -> {
                    String name = buildDisplayName(telegramUser);
                    User user = new User(telegramUser.getId(), name, telegramUser.getUserName());
                    userRepository.save(user);
                    log.info("Registered new user: {} ({})", name, telegramUser.getId());
                    return user;
                });
    }

    /**
     * Builds a human-readable display name from Telegram's first + last name fields.
     * lastName is optional on Telegram, so it's only appended when present.
     */
    private String buildDisplayName(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        String name = telegramUser.getFirstName();
        if (telegramUser.getLastName() != null) {
            name = name + " " + telegramUser.getLastName();
        }
        return name.trim();
    }
}
