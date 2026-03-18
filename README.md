# DiscoveryBot

A Telegram bot that lets group members save and query discoveries (food places, travel destinations, books, songs, videos, articles, etc.) using `/save` and natural language `/query`.

## Features

- **Save Discoveries** — Share links or descriptions of things you discover
- **Smart Enrichment** — Auto-extracts metadata from Google Places, YouTube, Spotify, and web pages
- **Natural Language Search** — Ask questions about saved discoveries in plain English
- **Group Sharing** — All discoveries are shared with your Telegram group
- **Admin Controls** — Delete entries, reset data (admin only)

## Getting Started

### Prerequisites
- Java 17+
- PostgreSQL 15
- Maven 3.8+
- Docker (optional, for running PostgreSQL locally)

### Setup

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd discovery-bot
   ```

2. **Create `.env` file**
   ```bash
   cp .env.example .env
   ```

   Fill in your `.env` with the following keys:

   | Key | Description |
   |-----|-------------|
   | `TELEGRAM_BOT_TOKEN` | Get from [@BotFather](https://t.me/botfather) on Telegram |
   | `TELEGRAM_BOT_USERNAME` | Your bot's Telegram username |
   | `OPENROUTER_API_KEY` | Get from [openrouter.ai](https://openrouter.ai) |
   | `GOOGLE_PLACES_API` | Get from [Google Cloud Console](https://console.cloud.google.com) |
   | `GOOGLE_YOUTUBE_API_KEY` | Get from [Google Cloud Console](https://console.cloud.google.com) |
   | `POSTGRES_USER` | PostgreSQL username (default: `postgres`) |
   | `POSTGRES_PASSWORD` | PostgreSQL password |

3. **Start PostgreSQL**
   ```bash
   docker-compose up -d
   ```

4. **Build and run the bot**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

   The bot will start listening for Telegram messages.

## Using the Bot

### Commands

- `/save <link or text>` — Save a discovery
- `/query <question>` — Ask about saved discoveries (e.g., "best coffee shops?")
- `/list` — Show 10 most recent items
- `/delete <id>` — Delete an entry (admin only)
- `/reset` — Delete all group data (admin only)
- `/help` — Show available commands

### Example Usage

**Save a discovery:**
```
User: /save https://www.example-restaurant.com
Bot: Saved! Amazing Italian restaurant near downtown with 4.8★ rating
```

**Query discoveries:**
```
User: /query best places to eat near the mall?
Bot: Based on your saved discoveries, here are the best places:
1. Mario's Pizzeria - Great pasta, cozy atmosphere
2. Golden Garden - Modern fusion, highly rated
...
```

## Tech Stack

- Spring Boot 3.2 with Java 17
- PostgreSQL database
- Telegram Bot API (long polling)
- OpenRouter for AI reasoning
- Google Places, YouTube, and Spotify APIs for enrichment
- Jsoup for web scraping

## Troubleshooting

**Bot not responding?**
- Check that `TELEGRAM_BOT_TOKEN` is correct in `.env`
- Verify PostgreSQL is running: `docker-compose ps`
- Check logs: `docker-compose logs postgres`

**Enrichment not working?**
- Verify API keys are correct in `.env`
- Check internet connection
- Bot will fall back to asking you for a description

**PostgreSQL connection error?**
- Ensure `.env` has correct `POSTGRES_USER` and `POSTGRES_PASSWORD`
- Restart PostgreSQL: `docker-compose restart postgres`

---

**Questions?** Open an issue on GitHub.
