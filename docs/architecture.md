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
YouTubeEnricher  GooglePlaces  [Phase 5]
    |            Enricher      OpenGraph
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
    v (both enrichers)
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
  "Saved! <summary>"

On enricher failure:
  "Could not fetch details. Tell me about it in your own words"
On no enricher (generic URL, pre-Phase 5):
  "Tell me about this in your own words"
```

---

## Query Flow (Phase 8)

```
User: /query <natural language>
         |
         v
Stage 1: AI extracts filters (category, tags, location, date range)
         |
         v
Stage 2: Java builds parameterized SQL scoped to group_id
         |
         v
Stage 3: AI reasons over result set, replies conversationally
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
| `OpenRouterServiceImpl` | All AI calls — extraction and querying |
| `DiscoveryEntryServiceImpl` | Persist to PostgreSQL |
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
   | extracted_data (JSONB) | category | source | tags (TEXT[]) | created_at
```

---

## Build Phases

| Phase | Status | Description |
|---|---|---|
| 1 | done | Telegram bot skeleton |
| 2 | done | User and Group auto-registration |
| 3 | done | /save command, enrichment chain skeleton, AI extraction |
| 4 | done | YouTube and Google Places enrichers |
| 5 | next | Open Graph enricher (Jsoup) |
| 6 | - | Conversation state management + timeout |
| 7 | done | Persist to PostgreSQL with JSONB |
| 8 | - | /query with two-stage AI reasoning |
| 9 | - | /list, /delete, /reset + admin controls |
| 10 | - | Error handling, edge cases, resilience |
| 11 | - | Deploy to Render + Neon PostgreSQL |
