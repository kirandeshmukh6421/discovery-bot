package com.discoverybot.service;

import com.discoverybot.model.Group;
import com.discoverybot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface CommandHandlerService {

    String handle(Update update, User user, Group group);
}
