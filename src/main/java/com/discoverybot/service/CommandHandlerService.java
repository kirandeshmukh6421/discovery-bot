package com.discoverybot.service;

import com.discoverybot.model.Group;
import com.discoverybot.model.User;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface CommandHandlerService {

    /**
     * Routes the incoming update to the appropriate command handler.
     *
     * @return the text to send back to the user, or null if no reply is needed
     */
    String handle(Update update, User user, Group group);
}
