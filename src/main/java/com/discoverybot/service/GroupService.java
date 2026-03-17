package com.discoverybot.service;

import com.discoverybot.model.Group;
import com.discoverybot.model.Role;
import com.discoverybot.model.User;
import org.telegram.telegrambots.meta.api.objects.Chat;

public interface GroupService {
    Group getOrCreate(Chat telegramChat);
    void registerUserInGroup(User user, Group group);
    Role getUserRole(User user, Group group);
}
