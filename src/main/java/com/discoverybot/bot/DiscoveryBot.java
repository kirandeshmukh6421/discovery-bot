package com.discoverybot.bot;

import com.discoverybot.model.Group;
import com.discoverybot.model.User;
import com.discoverybot.service.CommandHandlerService;
import com.discoverybot.service.GroupService;
import com.discoverybot.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Core bot class. Receives all Telegram updates via long polling and dispatches them.
 *
 * Responsibilities:
 * - Auto-register user and group on every message (Phase 2)
 * - Route all text to CommandHandlerService (Phase 3)
 * - Send the reply returned by the handler
 */
@Slf4j
@Component
public class DiscoveryBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final UserService userService;
    private final GroupService groupService;
    private final CommandHandlerService commandHandlerService;

    public DiscoveryBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            UserService userService,
            GroupService groupService,
            CommandHandlerService commandHandlerService) {
        super(botToken);
        this.botUsername = botUsername;
        this.userService = userService;
        this.groupService = groupService;
        this.commandHandlerService = commandHandlerService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        var message = update.getMessage();
        var telegramUser = message.getFrom();
        var telegramChat = message.getChat();

        User user = userService.getOrCreate(telegramUser);
        Group group = groupService.getOrCreate(telegramChat);
        groupService.registerUserInGroup(user, group);

        log.info("Message from {} in {}: {}", user.getName(), group.getName(), message.getText());

        String response = commandHandlerService.handle(update, user, group);
        if (response != null) {
            sendMessage(telegramChat.getId().toString(), response);
        }
    }

    public void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableMarkdown(true);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}: {}", chatId, e.getMessage());
        }
    }
}
