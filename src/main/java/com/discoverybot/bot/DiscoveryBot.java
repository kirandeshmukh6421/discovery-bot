package com.discoverybot.bot;

import com.discoverybot.model.Group;
import com.discoverybot.model.User;
import com.discoverybot.service.CommandHandlerService;
import com.discoverybot.service.GroupService;
import com.discoverybot.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;

/**
 * Core bot class. Receives Telegram updates via webhook and dispatches them.
 *
 * Responsibilities:
 * - Auto-register user and group on every message
 * - Route all text to CommandHandlerService
 * - Send replies back to Telegram via REST API
 */
@Slf4j
@Component
public class DiscoveryBot {

    private final String botToken;
    private final UserService userService;
    private final GroupService groupService;
    private final CommandHandlerService commandHandlerService;
    private final WebClient webClient;

    public DiscoveryBot(
            @Value("${telegram.bot.token}") String botToken,
            UserService userService,
            GroupService groupService,
            CommandHandlerService commandHandlerService,
            WebClient.Builder webClientBuilder) {
        this.botToken = botToken;
        this.userService = userService;
        this.groupService = groupService;
        this.commandHandlerService = commandHandlerService;
        this.webClient = webClientBuilder.baseUrl("https://api.telegram.org").build();
    }

    @Async
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        var message = update.getMessage();
        var telegramUser = message.getFrom();
        var telegramChat = message.getChat();

        // telegramUser is null for anonymous channel posts — skip them
        if (telegramUser == null || telegramChat == null) {
            return;
        }

        try {
            User user = userService.getOrCreate(telegramUser);
            Group group = groupService.getOrCreate(telegramChat);
            groupService.registerUserInGroup(user, group);

            log.info("Message from {} in {}: {}", user.getName(), group.getName(), message.getText());

            String response = commandHandlerService.handle(update, user, group);
            if (response != null) {
                sendMessage(telegramChat.getId().toString(), response);
            }
        } catch (Exception e) {
            log.error("Unhandled error processing update from chat {}: {}", telegramChat.getId(), e.getMessage(), e);
            sendMessage(telegramChat.getId().toString(), "⚠️ Something went wrong. Please try again.");
        }
    }

    public void sendMessage(String chatId, String text) {
        try {
            webClient.post()
                    .uri("/bot" + botToken + "/sendMessage")
                    .bodyValue(Map.of(
                            "chat_id", chatId,
                            "text", text,
                            "parse_mode", "Markdown"
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.error("Failed to send message to chat {}: {}", chatId, e.getMessage());
        }
    }
}
