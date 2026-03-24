package com.discoverybot.service.impl;

import com.discoverybot.dto.EnrichmentResult;
import com.discoverybot.dto.ExtractionResult;
import com.discoverybot.dto.PendingUserDescription;
import com.discoverybot.model.DiscoveryEntry;
import com.discoverybot.model.Group;
import com.discoverybot.model.Role;
import com.discoverybot.model.Source;
import com.discoverybot.model.User;
import com.discoverybot.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            /ping — wake up the bot
            /help — show this message
            """;

    private final EnricherService enricherService;
    private final OpenRouterService openRouterService;
    private final DiscoveryEntryService discoveryEntryService;
    private final GroupService groupService;
    private final ConversationStateService conversationStateService;
    private final QueryService queryService;
    private final ObjectMapper objectMapper;

    @Override
    public String handle(Update update, User user, Group group) {
        String text = stripBotMention(update.getMessage().getText().trim());

        if (text.startsWith("/save")) {
            return handleSave(text, user, group);
        }
        if (text.startsWith("/query")) {
            return handleQuery(text, group);
        }
        if (text.startsWith("/list")) {
            return handleList(group);
        }
        if (text.startsWith("/delete")) {
            return handleDelete(text, user, group);
        }
        if (text.startsWith("/reset")) {
            return handleReset(user, group);
        }
        if (text.startsWith("/ping")) {
            return "I'm up and running! ✅";
        }
        if (text.startsWith("/help") || text.equals("/start")) {
            return HELP_TEXT;
        }

        if (isReplyToBot(update) && conversationStateService.hasState(user.getTelegramId())) {
            return handlePendingDescription(text, user, group);
        }

        return null;
    }

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
                    new PendingUserDescription(url, userNote, java.time.Instant.now())
                );
                if (result.isFailed()) {
                    return "⚠️ " + result.failureReason() + ". ↩️ Reply to this message and describe it in your own words 👇";
                }
                return "↩️ Reply to this message and describe it in your own words 👇";
            }
            ExtractionResult extraction = result.extractionResult();
            discoveryEntryService.save(user, group, url, userNote, extraction, result.source());
            return confirmMessage(extraction);
        } else {
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
            return null;
        }

        log.info("Processing user description for user {} on URL: {}", user.getTelegramId(), pending.url());

        ExtractionResult extraction = openRouterService.extractDiscovery(userDescription);

        if (extraction == null) {
            log.warn("OpenRouter extraction failed for user description, saving raw input");
            discoveryEntryService.save(user, group, pending.url(), pending.userNote(), null, Source.USER_NOTE);
            conversationStateService.clearState(user.getTelegramId());
            return "✅ Saved! (Couldn't extract details, but I saved your input)";
        }

        discoveryEntryService.save(user, group, pending.url(), pending.userNote(), extraction, Source.AI_EXTRACTED);
        conversationStateService.clearState(user.getTelegramId());

        return confirmMessage(extraction);
    }

    private String handleQuery(String text, Group group) {
        String query = removeCommand(text, "/query").trim();
        if (query.isBlank()) {
            return "Usage: /query <question>\nExample: /query any good ramen spots?";
        }
        return queryService.query(group, query);
    }

    private String handleList(Group group) {
        var entries = discoveryEntryService.listRecent(group);
        if (entries.isEmpty()) {
            return "No discoveries saved yet.";
        }
        StringBuilder sb = new StringBuilder("📋 *Recent discoveries*\n\n");
        for (var entry : entries) {
            String summary = entry.getExtractedData() != null ? extractSummary(entry) : entry.getRawInput();
            String category = entry.getCategory() != null ? entry.getCategory() : "misc";
            sb.append("*").append(summary).append("*").append("\n");
            sb.append("🏷 ").append(category).append("  •  🆔 ").append(entry.getId()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String handleDelete(String text, User user, Group group) {
        if (!isAdmin(user, group)) {
            return "⛔ You don't have permission to delete entries.";
        }
        String idStr = removeCommand(text, "/delete").trim();
        if (idStr.isBlank()) {
            return "Usage: /delete <id>";
        }
        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return "Usage: /delete <id>";
        }
        boolean deleted = discoveryEntryService.delete(id, group);
        return deleted ? "✅ Deleted." : "❌ Entry not found.";
    }

    private String handleReset(User user, Group group) {
        if (!isAdmin(user, group)) {
            return "⛔ You don't have permission to reset group data.";
        }
        discoveryEntryService.reset(group);
        return "✅ All discoveries cleared.";
    }

    private String extractSummary(DiscoveryEntry entry) {
        try {
            JsonNode node = objectMapper.readTree(entry.getExtractedData());
            JsonNode summaryNode = node.get("summary");
            if (summaryNode != null && !summaryNode.isNull()) {
                return summaryNode.asText();
            }
        } catch (Exception e) {
            log.debug("Could not parse extractedData for entry {}", entry.getId());
        }
        return entry.getRawInput();
    }

    private boolean isReplyToBot(Update update) {
        var replyTo = update.getMessage().getReplyToMessage();
        return replyTo != null && replyTo.getFrom() != null && replyTo.getFrom().getIsBot();
    }

    private boolean isAdmin(User user, Group group) {
        return groupService.getUserRole(user, group) == Role.ADMIN;
    }

    private String removeCommand(String text, String command) {
        if (text.startsWith(command)) {
            return text.substring(command.length());
        }
        return text;
    }

    private String stripBotMention(String text) {
        return text.replaceFirst("^(/\\w+)@\\w+", "$1");
    }
}
