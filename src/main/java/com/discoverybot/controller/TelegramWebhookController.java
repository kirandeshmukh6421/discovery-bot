package com.discoverybot.controller;

import com.discoverybot.bot.DiscoveryBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Receives incoming updates from Telegram via webhook.
 * Returns 200 immediately and hands off processing to DiscoveryBot asynchronously.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final DiscoveryBot discoveryBot;

    @PostMapping("/telegram")
    public ResponseEntity<Void> webhook(@RequestBody Update update) {
        discoveryBot.onUpdateReceived(update);
        return ResponseEntity.ok().build();
    }
}
