package com.discoverybot.service.impl;

import com.discoverybot.dto.EnrichmentResult;
import com.discoverybot.dto.ExtractionResult;
import com.discoverybot.model.Group;
import com.discoverybot.model.Role;
import com.discoverybot.model.Source;
import com.discoverybot.model.User;
import com.discoverybot.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandHandlerServiceImpl implements CommandHandlerService {

    private static final String HELP_TEXT = """
            📖 *DiscoveryBot commands*

            /save <link or text> — save a discovery
            /query <question> — ask about saved discoveries
            /list — show 10 most recently saved items
            /delete <id> — delete an entry (admin only)
            /reset — clear all group data (admin only)
            /help — show this message
            """;

    private final EnricherService enricherService;
    private final OpenRouterService openRouterService;
    private final DiscoveryEntryService discoveryEntryService;
    private final GroupService groupService;

    @Override
    public String handle(Update update, User user, Group group) {
        String text = update.getMessage().getText().trim();

        if (text.startsWith("/save")) {
            return handleSave(text, user, group);
        }
        if (text.startsWith("/query")) {
            return handleQuery(text);
        }
        if (text.startsWith("/list")) {
            return "📋 /list coming in Phase 9.";
        }
        if (text.startsWith("/delete")) {
            return handleDelete(text, user, group);
        }
        if (text.startsWith("/reset")) {
            return handleReset(user, group);
        }
        if (text.startsWith("/help") || text.equals("/start")) {
            return HELP_TEXT;
        }

        // Not a command — will be used for conversation state replies in Phase 6
        return null;
    }

    // ── /save ──────────────────────────────────────────────────────────────────

    private String handleSave(String text, User user, Group group) {
        String input = removeCommand(text, "/save").trim();

        if (input.isBlank()) {
            return "Usage: /save <link or text>\nExample: /save https://example.com Great article!";
        }

        String url = EnricherServiceImpl.extractUrl(input);

        if (url != null) {
            String textBesideUrl = EnricherServiceImpl.textWithoutUrl(input, url);

            String userNote = textBesideUrl.isBlank() ? null : textBesideUrl;
            EnrichmentResult result = enricherService.enrich(url, userNote);
            if (result.needsUserDescription()) {
                if (result.isFailed()) {
                    return "⚠️ " + result.failureReason() + ". Tell me about it in your own words 👇";
                }
                // Phase 6 will track state and handle the reply
                return "Tell me about this in your own words 👇";
            }
            ExtractionResult extraction = result.extractionResult();
            discoveryEntryService.save(user, group, url, userNote, extraction, result.source());
            return confirmMessage(extraction);
        } else {
            // Plain text only → send to AI
            return saveWithAi(input, null, input, user, group);
        }
    }

    private String saveWithAi(String contentForAi, String userNote, String rawInput,
                               User user, Group group) {
        ExtractionResult extraction = openRouterService.extractDiscovery(contentForAi);
        discoveryEntryService.save(user, group, rawInput, userNote, extraction, Source.AI_EXTRACTED);
        return confirmMessage(extraction);
    }

    private String confirmMessage(ExtractionResult extraction) {
        if (extraction != null && extraction.summary() != null) {
            return "✅ Saved! " + extraction.summary();
        }
        return "✅ Saved!";
    }

    // ── /query ─────────────────────────────────────────────────────────────────

    private String handleQuery(String text) {
        String query = removeCommand(text, "/query").trim();
        if (query.isBlank()) {
            return "Usage: /query <question>\nExample: /query any good ramen spots?";
        }
        return "🔍 /query coming in Phase 8.";
    }

    // ── admin commands ─────────────────────────────────────────────────────────

    private String handleDelete(String text, User user, Group group) {
        if (!isAdmin(user, group)) {
            return "⛔ You don't have permission to delete entries.";
        }
        return "🗑️ /delete coming in Phase 9.";
    }

    private String handleReset(User user, Group group) {
        if (!isAdmin(user, group)) {
            return "⛔ You don't have permission to reset group data.";
        }
        return "⚠️ /reset coming in Phase 9.";
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private boolean isAdmin(User user, Group group) {
        return groupService.getUserRole(user, group) == Role.ADMIN;
    }

    private String removeCommand(String text, String command) {
        if (text.startsWith(command)) {
            return text.substring(command.length());
        }
        return text;
    }
}
