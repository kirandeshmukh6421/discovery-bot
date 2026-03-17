package com.discoverybot.config;

import com.discoverybot.bot.DiscoveryBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Spring configuration class for Telegram bot initialization.
 * Creates and manages the TelegramBotsApi bean that handles the bot's connection to Telegram.
 */
@Slf4j
@Configuration
public class BotConfig {

    /**
     * Creates and registers the Telegram bot with the Telegram API.
     * This bean is created during Spring startup and initializes long polling,
     * which continuously listens for new messages from Telegram servers.
     *
     * The bot will remain connected and listening until the application shuts down.
     *
     * @param discoveryBot The DiscoveryBot component to register
     * @return The initialized TelegramBotsApi instance
     * @throws TelegramApiException If registration with Telegram fails (e.g., invalid token)
     */
    @Bean
    TelegramBotsApi telegramBotsApi(DiscoveryBot discoveryBot) throws TelegramApiException {
        // Create the API instance with DefaultBotSession (uses long polling, not webhooks)
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);

        // Register the bot so it starts listening for updates
        api.registerBot(discoveryBot);

        // Log successful registration
        log.info("DiscoveryBot registered and polling for updates");

        return api;
    }
}
