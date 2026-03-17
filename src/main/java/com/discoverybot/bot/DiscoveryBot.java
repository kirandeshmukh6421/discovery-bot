package com.discoverybot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class DiscoveryBot extends TelegramLongPollingBot {

    private final String botUsername;

    /**
     * Constructor that initializes the bot with credentials from application.properties.
     * Uses constructor injection to receive the token and username from Spring config.
     *
     * @param botToken The Telegram bot token (read from telegram.bot.token property)
     * @param botUsername The bot's username (read from telegram.bot.username property)
     */
    public DiscoveryBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername) {
        super(botToken);
        this.botUsername = botUsername;
    }

    /**
     * Returns the bot's username as it appears on Telegram (e.g., "discvry_bot").
     * Required by the TelegramLongPollingBot interface.
     *
     * @return The bot's username
     */
    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Called automatically whenever the bot receives a message from Telegram.
     * Currently echoes back every text message the user sends.
     * In future phases, this will route messages to command handlers (/save, /query, etc.).
     *
     * @param update The incoming message update from Telegram
     */
    @Override
    public void onUpdateReceived(Update update) {
        // Ignore updates that aren't text messages
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        // Extract message data from the update
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        String userName = update.getMessage().getFrom().getFirstName();

        // Log the incoming message for debugging/monitoring
        log.info("Received message from {} in chat {}: {}", userName, chatId, text);

        // For now, just echo back the message
        // In future phases, this will parse commands like /save, /query, etc.
        String reply = "I received your message: " + text;

        // Prepare the response message
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(reply);

        // Send the response and handle any failures
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}: {}", chatId, e.getMessage());
        }
    }
}
