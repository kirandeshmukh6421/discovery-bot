# DiscoveryBot — Architecture

---

## Save Flow

```
User: /save <input>
         |
         v
  Extract URL from input?
         |
    yes  |  no
    -----+-----
    |          |
    v          v
URL+note?   Plain text
    |          |
    |          v
    |      OpenRouterService.extractDiscovery()
    |          |
    |          v
    |       Save (AI_EXTRACTED)
    |
    v
EnricherServiceImpl.enrich(url, userNote?)
         |
   detectLinkType()
         |
    -----+----------+-------------+
    |               |             |
    v               v             v
YOUTUBE        GOOGLE_MAPS    GENERIC_URL
    |               |             |
    v               v             v
YouTubeEnricher  GooglePlaces  OpenGraph
    |            Enricher      Enricher
    |               |          (Jsoup)
    |               |
    |   resolve short link
    |               |
    |   coords?----yes----> searchNearby (50m)
    |   |                        |
    |   no                       |
    |   |                        |
    |   v                        |
    | searchText <---------------+
    |
    v (all enrichers)
OpenRouterService.extractDiscovery(apiData + userNote?)
         |
         v
   ExtractionResult
(category, summary, tags, isPhysicalLocation)
         |
         v
  DiscoveryEntryService.save()
         |
         v
  EmbeddingService.embed(category | tags | summary)
         |
         v
  Store embedding vector(1536) in DB
         |
         v
  "✅ Saved! <summary>"

On enricher failure (YouTube/Maps):
  "Could not fetch details. Tell me about it in your own words"
On Open Graph unusable (login wall / blank):
  "Tell me about this in your own words"
```

---

## Query Flow

```
User: /query <natural language>
         |
         v
EmbeddingService.embed(userQuery)
         |
   success?
    yes  |  no
    |    v
    |  findRecent(group, 30)  <-- fallback
    |
    v
DiscoveryEntryRepository.findSimilar(groupId, queryVector, 15)
  ORDER BY embedding <=> queryVector  (cosine similarity)
         |
  0 results? --> fallback to findRecent(group, 30)
         |
         v
  Serialize top entries into context string:
  [1] Category: restaurant | Source: OPEN_GRAPH | Tags: pizza, bangalore
      Summary: Eleven Bakehouse serves NY-style pizza in Indiranagar
      Link: https://... | Note: "try the margherita" | Saved: 2026-03-20
         |
         v
OpenRouterService.answerQuery(context, userQuery)
  system: conversational assistant prompt
  user:   context + question
         |
         v
  Plain text reply (with links + notes for every entry mentioned)
```

---

## Auth Model

```
Any message received
         |
    -----+-----
    |          |
    v          v
UserService  GroupService
getOrCreate  getOrCreate
         |
         v
  registerUserInGroup()
         |
  first user in group? --> ADMIN
  else               --> MEMBER
```

---

## Component Responsibilities

| Component | Responsibility |
|---|---|
| `DiscoveryBot` | Receive Telegram updates, route to handlers |
| `CommandHandlerServiceImpl` | Parse commands, orchestrate save/query flow |
| `EnricherServiceImpl` | Detect link type, route to correct enricher |
| `YouTubeEnricher` | Fetch video/playlist data from YouTube API |
| `GooglePlacesEnricher` | Resolve Maps links, fetch place data from Places API |
| `OpenGraphEnricher` | Scrape og:title/description/site_name from generic URLs via Jsoup |
| `ConversationStateServiceImpl` | Track multi-step user flows (waiting for description) with 5-min auto-timeout |
| `OpenRouterServiceImpl` | All AI calls — extraction (JSON) and query answering (conversational) |
| `EmbeddingServiceImpl` | Generate 1536-dim embeddings via OpenRouter `/v1/embeddings` |
| `QueryServiceImpl` | Vector search → serialize context → AI answer |
| `DiscoveryEntryServiceImpl` | Persist to PostgreSQL, store embeddings after save |
| `UserServiceImpl` | Auto-register users by Telegram ID |
| `GroupServiceImpl` | Auto-register groups, manage roles |

---

## Data Flow for a YouTube Save

```
/save https://youtu.be/abc123 great food vlog
         |
         v
EnricherServiceImpl --> YOUTUBE
         |
         v
YouTubeEnricher
  GET /videos?id=abc123&part=snippet
         |
         v
  title + channel + description (trimmed to 1000 chars) + user note
         |
         v
  OpenRouter --> category, summary, tags, isPhysicalLocation
         |
         v
  append channel name to tags
         |
         v
  DiscoveryEntryService.save(source=YOUTUBE)
         |
         v
  EmbeddingService.embed() --> store vector(1536)
```

---

## Data Flow for a Google Maps Save

```
/save https://maps.app.goo.gl/xyz
         |
         v
EnricherServiceImpl --> GOOGLE_MAPS
         |
         v
GooglePlacesEnricher
  HttpURLConnection.followRedirect()
         |
         v
  resolved URL contains coords? --> searchNearby (50m radius)
  resolved URL contains name?   --> searchText
         |
         v
  name + address + rating + types + editorial summary
         |
         v
  OpenRouter --> category, summary, tags, isPhysicalLocation
         |
         v
  DiscoveryEntryService.save(source=GOOGLE_PLACES)
         |
         v
  EmbeddingService.embed() --> store vector(1536)
```

---

## Data Flow for an Open Graph Save

```
/save https://some-article.com my favourite read
         |
         v
EnricherServiceImpl --> GENERIC_URL
         |
         v
OpenGraphEnricher
  Jsoup.connect(url).userAgent("Mozilla/5.0 ...").get()
         |
         v
  og:title + og:description + og:site_name
         |
  useful? (title or description non-blank)
    yes  |  no
    |    v
    |  return empty --> EnrichmentResult.askUser()
    |                   "Tell me about this in your own words"
    v
  OpenRouter --> category, summary, tags, isPhysicalLocation
         |
         v
  DiscoveryEntryService.save(source=OPEN_GRAPH)
         |
         v
  EmbeddingService.embed() --> store vector(1536)
```

---

## Conversation State Management (Phase 6)

```
User starts /save but enricher asks for description
         |
         v
CommandHandler.setState(userId, WAITING_FOR_USER_DESCRIPTION)
         |
         v
ScheduledExecutorService schedules timeout:
  After 5 minutes, auto-clear state if not completed
         |
         v
Bot replies: "Tell me about this in your own words 👇"
         |
   [User has 5 minutes to reply]
   |
   v (user sends reply)
CommandHandler.hasState(userId) == true?
   |
   YES v
   getState(userId) → PendingUserDescription(url, note)
   Send user's text to OpenRouter
         |
         v
   ExtractionResult → save(source=AI_EXTRACTED)
   clearState(userId)
   "✅ Saved! ..."

   NO
   (State expired) → Ignore message
```

---

## Admin Commands (Phase 9)

```
/list
         |
         v
DiscoveryEntryService.listRecent(group)   <-- top 10 by created_at DESC
         |
         v
Format: bold summary + category + id per entry
         |
         v
"📋 Recent discoveries\n*<summary>*\n🏷 <category> • 🆔 <id>\n..."

/delete <id>
         |
    isAdmin?  no --> "⛔ You don't have permission"
         |
        yes
         |
    parse id (invalid/missing --> "Usage: /delete <id>")
         |
         v
DiscoveryEntryRepository.deleteByIdAndGroup(id, group)
  scoped to group — prevents cross-group deletes
         |
  found? yes --> "✅ Deleted."
         no  --> "❌ Entry not found."

/reset
         |
    isAdmin?  no --> "⛔ You don't have permission"
         |
        yes
         |
         v
DiscoveryEntryRepository.deleteAllByGroup(group)
         |
         v
"✅ All discoveries cleared."
```

---

## Database Schema

```
users
  id | telegram_id (unique) | name | username | created_at

groups
  id | telegram_group_id (unique) | name | created_at

user_groups
  id | user_id (FK) | group_id (FK) | role (ADMIN/MEMBER) | joined_at

discovery_entries
  id | group_id (FK) | added_by (FK) | raw_input | user_note
   | extracted_data (JSONB) | category | source | tags | created_at
   | embedding vector(1536)   <-- cosine similarity search (Phase 8)

Schema managed by Flyway:
  V1__init.sql  — baseline tables
  V2__pgvector.sql — CREATE EXTENSION vector, ADD COLUMN embedding, HNSW index
```

---

## Build Phases

| Phase | Status | Description |
|---|---|---|
| 1 | ✅ | Telegram bot skeleton |
| 2 | ✅ | User and Group auto-registration |
| 3 | ✅ | /save command, enrichment chain skeleton, AI extraction |
| 4 | ✅ | YouTube and Google Places enrichers |
| 5 | ✅ | Open Graph enricher (Jsoup) |
| 6 | ✅ | Conversation state management + 5-min timeout |
| 7 | ✅ | Persist to PostgreSQL with JSONB |
| 8 | ✅ | /query with pgvector semantic search + conversational AI answer |
| 9 | ✅ | /list, /delete, /reset + admin controls |
| 10 | — | Error handling, edge cases, resilience |
| 11 | — | Deploy to Render + Neon PostgreSQL |
