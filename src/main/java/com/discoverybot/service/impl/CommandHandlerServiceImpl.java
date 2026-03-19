package com.discoverybot.service.impl;

import com.discoverybot.dto.EnrichmentResult;
import com.discoverybot.dto.ExtractionResult;
import com.discoverybot.dto.PendingUserDescription;
import com.discoverybot.model.Group;
import com.discoverybot.model.Role;
import com.discoverybot.model.Source;
import com.discoverybot.model.User;
import com.discoverybot.service.*;
import com.discoverybot.state.ConversationState;
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
    private final ConversationStateService conversationStateService;

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

        // Check if user has a pending state (e.g., waiting for description)
        if (conversationStateService.hasState(user.getTelegramId())) {
            return handlePendingDescription(text, user, group);
        }

        // Not a command and no pending state — ignore
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
                conversationStateService.setState(
                    user.getTelegramId(),
                    ConversationState.WAITING_FOR_USER_DESCRIPTION,
                    new PendingUserDescription(url, userNote, java.time.Instant.now())
                );
                if (result.isFailed()) {
                    return "⚠️ " + result.failureReason() + ". Tell me about it in your own words 👇";
                }
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

    private String handlePendingDescription(String userDescription, User user, Group group) {
        PendingUserDescription pending = conversationStateService.getState(user.getTelegramId());
        if (pending == null) {
            return null;  // State was cleared (timeout)
        }

        log.info("Processing user description for user {} on URL: {}", user.getTelegramId(), pending.url());

        // Send user's description to OpenRouter for extraction
        ExtractionResult extraction = openRouterService.extractDiscovery(userDescription);

        if (extraction == null) {
            log.warn("OpenRouter extraction failed for user description, saving raw input");
            // Save raw input even if extraction failed
            discoveryEntryService.save(user, group, pending.url(), pending.userNote(), null, Source.USER_NOTE);
            conversationStateService.clearState(user.getTelegramId());
            return "✅ Saved! (Couldn't extract details, but I saved your input)";
        }

        // Save with source = AI_EXTRACTED
        discoveryEntryService.save(user, group, pending.url(), pending.userNote(), extraction, Source.AI_EXTRACTED);
        conversationStateService.clearState(user.getTelegramId());

        return confirmMessage(extraction);
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
