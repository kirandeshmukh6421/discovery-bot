# DiscoveryBot

A Telegram bot that lets group members save and query discoveries: food places, travel destinations, books, videos, articles, and more.

## What it does

Send a link or describe something and the bot saves it for your group. Later, ask anything in plain English and it finds what you're looking for.

```
/save https://maps.app.goo.gl/xyz  great ramen spot
-> Saved! Ichiran Ramen, authentic tonkotsu ramen chain known for solo dining booths.

/query good ramen spots
-> Your group saved Ichiran Ramen in Dotonbori, known for rich tonkotsu broth.
   Link: https://maps.app.goo.gl/xyz | Note: "great ramen spot"
```

## Getting Started

1. Open Telegram and search for **@discvry_bot**
2. Add the bot to your group
3. The first person to add the bot becomes the group admin

## Commands

| Command | Description |
|---------|-------------|
| `/save <link or text>` | Save a discovery |
| `/query <question>` | Ask about saved discoveries |
| `/list` | Show 10 most recently saved items |
| `/delete <id>` | Delete an entry (admin only) |
| `/reset` | Clear all group data (admin only) |
| `/help` | Show available commands |

## How saving works

- **Google Maps link:** fetches place name, address, rating, and description
- **YouTube link:** fetches title, channel, and description
- **Any other link:** scrapes Open Graph metadata
- **Plain text:** AI extracts structured info directly
- **Enrichment fails:** bot asks you to describe it in your own words

## Tech Stack

- Java 17, Spring Boot 3.5
- PostgreSQL 15 + pgvector (semantic search)
- OpenRouter API (AI extraction and querying)
- Google Places API, YouTube Data API v3
- Telegram Bot API (webhook mode)
- Docker + Docker Compose

---

## Self-Hosting

### Prerequisites

- Docker Desktop
- A Telegram bot token from [@BotFather](https://t.me/botfather)
- API keys for OpenRouter, Google Places, and YouTube

### 1. Clone the repository

```bash
git clone <repo-url>
cd discovery-bot
```

### 2. Create `.env` file

```bash
cp .env.example .env
```

| Key | Description |
|-----|-------------|
| `TELEGRAM_BOT_TOKEN` | From [@BotFather](https://t.me/botfather) |
| `TELEGRAM_BOT_USERNAME` | Your bot's username |
| `OPENROUTER_API_KEY` | From [openrouter.ai](https://openrouter.ai) |
| `OPENROUTER_MODEL` | e.g. `openai/gpt-4o-mini` |
| `OPENROUTER_EMBEDDING_MODEL` | e.g. `openai/text-embedding-3-small` |
| `GOOGLE_PLACES_API_KEY` | From [Google Cloud Console](https://console.cloud.google.com) |
| `YOUTUBE_API_KEY` | From [Google Cloud Console](https://console.cloud.google.com) |
| `POSTGRES_USER` | PostgreSQL username |
| `POSTGRES_PASSWORD` | PostgreSQL password |

### 3. Start the app

```bash
docker-compose up --build
```

Starts both PostgreSQL and the bot. On subsequent runs use `docker-compose up`.

### 4. Register the webhook

The bot uses webhooks so Telegram needs a public HTTPS URL to forward messages to. Use [ngrok](https://ngrok.com) to expose your local server:

```bash
ngrok http 8080
```

Then register the URL with Telegram:

```
https://api.telegram.org/bot<TOKEN>/setWebhook?url=https://your-url.ngrok-free.app/telegram
```

You should get back `{"ok":true}`. Add the bot to a Telegram group and start saving.

### Troubleshooting

**Bot not responding:** verify the webhook at `https://api.telegram.org/bot<TOKEN>/getWebhookInfo`

**Enrichment not working:** check API keys in `.env`. The bot falls back to asking for a manual description if enrichment fails.

**Database error:** check `POSTGRES_USER` and `POSTGRES_PASSWORD`. Restart with `docker-compose restart postgres`.
