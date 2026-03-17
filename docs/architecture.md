# DiscoveryBot — Code Walkthrough

A living document explaining what each file does and how the pieces fit together.

---

## Entry Points

### `DiscoveryBotApplication.java`
Spring Boot entry point. Sets the default JVM timezone to `Asia/Kolkata` before the context starts so all `LocalDateTime` values stored in the DB are in IST.

---

### `bot/DiscoveryBot.java`
The core bot class. Extends `TelegramLongPollingBot` from the telegrambots library.

**Long polling** means the app repeatedly asks Telegram "any new messages?" on a background thread — no public URL needed, works locally.

Key things:
- Constructor takes `botToken` and `botUsername` from `application.properties` via `@Value`
- `super(botToken)` authenticates the bot with Telegram's API
- `onUpdateReceived(Update update)` is called by the library every time a message arrives
- An `Update` can be anything — text, photo, button click. The guard `!update.hasMessage() || !update.getMessage().hasText()` ignores everything that isn't plain text
- On every message: calls `UserService.getOrCreate()` → `GroupService.getOrCreate()` → `GroupService.registerUserInGroup()` to auto-register
- `chatId` identifies where to send the reply back (group or DM)
- `execute(SendMessage)` makes the actual HTTP call to Telegram's API — inherited from parent
- Send errors are caught and logged — never crashes the bot

**Phase 3+**: Will route commands (`/save`, `/query`, etc.) to `CommandHandlerService`.

---

## Config

### `config/BotConfig.java`
Registers the bot with Telegram on startup.

- `TelegramBotsApi` is the library's central registry
- `DefaultBotSession` starts the background long-polling thread
- `registerBot(discoveryBot)` hands the bot instance to that session so it starts receiving updates
- Without this file, `DiscoveryBot` exists as a Spring bean but never connects to Telegram

### `config/AppConfig.java`
Declares a shared `WebClient.Builder` bean used by all services that make external HTTP calls (enrichers, OpenRouterService, etc.). Consumers inject the builder and call `.build()` to get their own configured `WebClient` instance.

---

## Services

### `service/UserService.java` (interface)
Contract for user identity resolution. Single method: `getOrCreate(telegramUser)`.

### `service/impl/UserServiceImpl.java`
- Looks up user by `telegram_id` (Telegram's stable numeric ID)
- If not found, inserts a new row with first + last name and Telegram username
- First contact is invisible to the user — no welcome message, just silent registration
- `buildDisplayName()` concatenates first + optional last name

### `service/GroupService.java` (interface)
Contract for group identity resolution and membership management. Methods:
- `getOrCreate(chat)` — upsert group
- `registerUserInGroup(user, group)` — upsert membership with role
- `getUserRole(user, group)` — return ADMIN or MEMBER

### `service/impl/GroupServiceImpl.java`
- `getOrCreate()`: looks up group by `telegram_group_id`; inserts on first encounter
- `registerUserInGroup()`: idempotent — exits early if membership already exists. First user in a group → `ADMIN`, rest → `MEMBER`
- `getUserRole()`: returns role from `user_groups`; falls back to `MEMBER` if no record

**Future services** (not yet implemented):

| Service | Phase | Purpose |
|---|---|---|
| `CommandHandlerService` | 3 | Routes `/save`, `/query`, etc. |
| `ConversationStateService` | 6 | Tracks WAITING_FOR_USER_DESCRIPTION state per user |
| `OpenRouterService` | 3 | All AI calls via OpenRouter API |
| `EnricherService` | 3 | Orchestrates the enrichment chain |
| `DiscoveryEntryService` | 7 | Persists discovery entries |
| `QueryService` | 8 | AI-powered querying over saved entries |

---

## Enrichers (Phase 4–5)

| Enricher | Triggers on |
|---|---|
| `GooglePlacesEnricher` | Google Maps links or AI-detected physical locations |
| `YouTubeEnricher` | YouTube links → title, channel, duration, views |
| `SpotifyEnricher` | Spotify links |
| `OpenGraphEnricher` | Any URL → og:title, og:description via Jsoup |

---

## Models

### `model/User.java`
Maps to the `users` table. Fields: `id`, `telegram_id` (unique), `name`, `username`, `created_at`.
`telegram_id` is Telegram's own numeric user ID — used as the lookup key.

### `model/Group.java`
Maps to the `groups` table. Fields: `id`, `telegram_group_id` (unique), `name`, `created_at`.
Works for both group chats (negative IDs) and private DMs (positive IDs).

### `model/Role.java`
Enum: `ADMIN` | `MEMBER`. Stored as a string in the `user_groups.role` column.

### `model/UserGroup.java`
Join table `user_groups` — links a `User` to a `Group` with a `Role`.
Has a unique constraint on `(user_id, group_id)` to prevent duplicate memberships.

### `model/DiscoveryEntry.java` _(Phase 7)_
Stores saved discoveries. Includes a JSONB `extracted_data` column for flexible AI-extracted metadata.

---

## State

### `state/ConversationState.java` _(Phase 6)_
Enum: `IDLE`, `WAITING_FOR_USER_DESCRIPTION`

Used when the bot asks "Tell me about this in your own words" after a failed enrichment chain. State is stored in a `ConcurrentHashMap<Long, ConversationState>` keyed by `userId`. Auto-clears after 5 minutes.

---

## Repositories

### `repository/UserRepository.java`
Extends `JpaRepository<User, Long>`. Custom query: `findByTelegramId(Long telegramId)`.

### `repository/GroupRepository.java`
Extends `JpaRepository<Group, Long>`. Custom query: `findByTelegramGroupId(Long telegramGroupId)`.

### `repository/UserGroupRepository.java`
Extends `JpaRepository<UserGroup, Long>`. Custom queries:
- `findByUserAndGroup(User, Group)` — check if membership exists
- `existsByGroup(Group)` — check if a group has any members yet (used for first-admin logic)

### `repository/DiscoveryEntryRepository.java` _(Phase 7)_
Will support queries scoped by `group_id` for `/query` and `/list`.
