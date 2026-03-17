# DiscoveryBot — Code Walkthrough

A living document explaining what each file does and how the pieces fit together.

---

## Entry Points

### `bot/DiscoveryBot.java`
The core bot class. Extends `TelegramLongPollingBot` from the telegrambots library.

**Long polling** means the app repeatedly asks Telegram "any new messages?" on a background thread — no public URL needed, works locally.

Key things:
- Constructor takes `botToken` and `botUsername` from `application.properties` via `@Value`
- `super(botToken)` authenticates the bot with Telegram's API
- `onUpdateReceived(Update update)` is called by the library every time a message arrives
- An `Update` can be anything — text, photo, button click. The guard `!update.hasMessage() || !update.getMessage().hasText()` ignores everything that isn't plain text
- `chatId` identifies where to send the reply back (group or DM)
- `execute(SendMessage)` makes the actual HTTP call to Telegram's API — inherited from parent class
- Wrapped in try/catch so a failed send never crashes the bot

Currently (Phase 1): echoes back whatever it receives. Will be wired to `CommandHandlerService` in Phase 2+.

---

### `config/BotConfig.java`
Registers the bot with Telegram on startup.

- `TelegramBotsApi` is the library's central registry
- `DefaultBotSession` starts the background long-polling thread
- `registerBot(discoveryBot)` hands the bot instance to that session so it starts receiving updates
- Without this file, `DiscoveryBot` exists as a Spring bean but never connects to Telegram

---

## Config

### `config/AppConfig.java`
_(not yet explored)_

---

## Services

### `service/CommandHandlerService.java`
_(not yet explored)_

### `service/ConversationStateService.java`
_(not yet explored)_

### `service/OpenRouterService.java`
_(not yet explored)_

### `service/EnricherService.java`
_(not yet explored)_

### `service/DiscoveryEntryService.java`
_(not yet explored)_

### `service/QueryService.java`
_(not yet explored)_

### `service/UserService.java`
_(not yet explored)_

### `service/GroupService.java`
_(not yet explored)_

---

## Enrichers

### `service/enricher/GooglePlacesEnricher.java`
_(not yet explored)_

### `service/enricher/YouTubeEnricher.java`
_(not yet explored)_

### `service/enricher/SpotifyEnricher.java`
_(not yet explored)_

### `service/enricher/OpenGraphEnricher.java`
_(not yet explored)_

---

## Models

### `model/User.java`
_(not yet explored)_

### `model/Group.java`
_(not yet explored)_

### `model/UserGroup.java`
_(not yet explored)_

### `model/DiscoveryEntry.java`
_(not yet explored)_

---

## State

### `state/ConversationState.java`
Enum with values: `IDLE`, `WAITING_FOR_USER_DESCRIPTION`

Used when the bot asks "Tell me about this in your own words" and waits for a follow-up reply. Stored in a `ConcurrentHashMap` keyed by `userId`. Auto-clears after 5 minutes.

---

## Repositories

### `repository/UserRepository.java`
_(not yet explored)_

### `repository/GroupRepository.java`
_(not yet explored)_

### `repository/UserGroupRepository.java`
_(not yet explored)_

### `repository/DiscoveryEntryRepository.java`
_(not yet explored)_
