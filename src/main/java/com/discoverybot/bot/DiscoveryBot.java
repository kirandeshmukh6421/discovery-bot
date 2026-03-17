package com.discoverybot.bot;

import com.discoverybot.model.Group;
import com.discoverybot.model.User;
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
 * Long polling: the telegrambots library keeps an open connection to Telegram's servers
 * on a background thread, fetching new updates as they arrive. No public URL needed —
 * works in local dev and on any server without firewall config.
 *
 * All messages from all chats (DMs and groups) arrive here through onUpdateReceived().
 * Phase 2: auto-registers users and groups on first contact.
 * Phase 3+: will route /save, /query, /list, etc. to CommandHandlerService.
 */
@Slf4j
@Component
public class DiscoveryBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final UserService userService;
    private final GroupService groupService;

    /**
     * Constructor injection — credentials come from application.properties / .env.
     * botToken is passed to the parent class which uses it to authenticate all API calls.
     */
    public DiscoveryBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            UserService userService,
            GroupService groupService) {
        super(botToken);
        this.botUsername = botUsername;
        this.userService = userService;
        this.groupService = groupService;
    }

    /** Required by TelegramLongPollingBot — used by the library to identify this bot. */
    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Entry point for every incoming Telegram update.
     *
     * Current flow (Phase 2):
     * 1. Ignore non-text updates (photos, stickers, joins, etc.)
     * 2. Auto-register user and group (idempotent — safe to call on every message)
     * 3. Echo back the message (placeholder until Phase 3 command routing)
     *
     * @param update The incoming update from Telegram
     */
    @Override
    public void onUpdateReceived(Update update) {
        // Only handle plain text messages; ignore photos, stickers, join events, etc.
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        var message = update.getMessage();
        var telegramUser = message.getFrom();
        var telegramChat = message.getChat();

        // Auto-register user and group on every message (no-op if already registered)
        User user = userService.getOrCreate(telegramUser);
        Group group = groupService.getOrCreate(telegramChat);
        groupService.registerUserInGroup(user, group);

        String text = message.getText();
        log.info("Received message from {} in chat {}: {}", user.getName(), group.getName(), text);

        // TODO Phase 3: route to CommandHandlerService based on text starting with /
        sendMessage(telegramChat.getId().toString(), "I received your message: " + text);
    }

    /**
     * Sends a plain text message to the given chat.
     * Errors are caught and logged — the bot must never crash on a failed send.
     *
     * @param chatId Telegram chat ID (as string)
     * @param text   Message text to send
     */
    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}: {}", chatId, e.getMessage());
        }
    }
}
