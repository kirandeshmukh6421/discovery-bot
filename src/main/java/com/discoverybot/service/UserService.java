package com.discoverybot.service;

import com.discoverybot.model.User;

public interface UserService {
    User getOrCreate(org.telegram.telegrambots.meta.api.objects.User telegramUser);
}
