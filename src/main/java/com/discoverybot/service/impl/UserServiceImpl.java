package com.discoverybot.service.impl;

import com.discoverybot.model.User;
import com.discoverybot.repository.UserRepository;
import com.discoverybot.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UserServiceImpl implements UserService {

    UserRepository userRepository;

    @Override
    public User getOrCreate(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        return userRepository.findByTelegramId(telegramUser.getId())
                .orElseGet(() -> {
                    String name = buildDisplayName(telegramUser);
                    User user = new User(telegramUser.getId(), name, telegramUser.getUserName());
                    userRepository.save(user);
                    log.info("Registered new user: {} ({})", name, telegramUser.getId());
                    return user;
                });
    }

    private String buildDisplayName(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        String name = telegramUser.getFirstName();
        if (telegramUser.getLastName() != null) {
            name = name + " " + telegramUser.getLastName();
        }
        return name.trim();
    }
}
