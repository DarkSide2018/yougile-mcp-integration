# YouGile Alert Bot — Architecture & Implementation Plan

## Overview

Система для приема алертов через Telegram, обработки через OpenCode AI, обогащения через RAG (PGvector) с шаблонами задач и создания задач в YouGile через MCP-сервер на Kotlin.

## Architecture

```
Telegram (Alert)
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│                   Alert Bot App (Spring Boot)                 │
│  ┌──────────────────┐   ┌──────────────────────────────────┐ │
│  │  Telegram Bot     │──►│  AlertProcessingService         │ │
│  │  (kotlin-         │   │  ┌────────────────────────────┐ │ │
│  │   telegram-bot)   │   │  │  OpenCode AI Analyzer     │ │ │
│  │                   │   │  │  (zen provider)           │ │ │
│  │  /testalert ──────┤   │  └───────────┬───────────────┘ │ │
│  └──────────────────┘   │              │                  │ │
│                          │              ▼                  │ │
│                          │  ┌────────────────────────────┐ │ │
│                          │  │  RAG Knowledge Base        │ │ │
│                          │  │  (PGvector)                │ │ │
│                          │  │  ┌──────────────────────┐  │ │ │
│                          │  │  │ Task Templates .md   │  │ │ │
│                          │  │  │ └→ Embeddings → PG   │  │ │ │
│                          │  │  └──────────────────────┘  │ │ │
│                          │  └───────────┬────────────────┘ │ │
│                          └──────────────┼──────────────────┘ │
│  ┌──────────────────────────────────────┴──────────────────┐ │
│  │              Spring AI MCP Client                       │ │
│  └──────────────────────┬──────────────────────────────────┘ │
└─────────────────────────┼────────────────────────────────────┘
                          │ MCP (JSON-RPC, Streamable HTTP)
                          ▼
┌──────────────────────────────────────────────────────────────┐
│              MCP Server (Kotlin / Spring AI MCP)              │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  Tools: create_yougile_task, list_yougile_boards,       │  │
│  │         list_yougile_users, list_yougile_columns        │  │
│  └────────────────────┬────────────────────────────────────┘  │
└───────────────────────┼────────────────────────────────────────┘
                        │ YouGile REST API v2 (HTTPS)
                        ▼
┌──────────────────────────────────────────────────────────────┐
│                     YouGile Cloud                            │
│              → Task created on board                         │
└──────────────────────────────────────────────────────────────┘
```

**Also**: OpenCode CLI can connect to the same MCP Server via `opencode.json` for manual task management.

---

## Technology Stack

| Component              | Technology                                      |
|------------------------|-------------------------------------------------|
| Language               | Kotlin (JVM)                                    |
| Build                  | Gradle Kotlin DSL (multi-module)                |
| Frameworks             | Spring Boot 3.x + Spring AI 2.0.0 MCP           |
| Telegram Bot           | kotlin-telegram-bot                             |
| MCP Server SDK         | Spring AI `spring-ai-starter-mcp-server`        |
| MCP Client SDK         | Spring AI `spring-ai-starter-mcp-client`        |
| HTTP Client            | Spring WebClient                                 |
| Embedding              | OpenAI-compatible (zen.opencode.ai)             |
| Vector Store           | PGvector (Spring AI pgvector-store)             |
| OpenCode Provider      | zen (built-in, zen.opencode.ai)                 |
| Project Mgmt API       | YouGile REST API v2                              |
| Database               | PostgreSQL 16 + pgvector extension              |
| Containerization       | Docker Compose                                   |

---

## Project Structure

```
yougile-alert-bot/
├── mcp-server/                          # MCP Server module
│   └── src/main/kotlin/com/yougile/mcp/
│       ├── YougileMcpServerApplication.kt
│       ├── config/YougileApiConfig.kt
│       └── tools/YougileTaskTools.kt
├── alert-bot-app/                       # Main application
│   └── src/main/kotlin/com/yougile/alertbot/
│       ├── AlertBotApplication.kt
│       ├── config/
│       │   ├── McpClientConfig.kt
│       │   └── OpenCodeConfig.kt
│       ├── model/Alert.kt
│       ├── telegram/
│       │   └── AlertTelegramBot.kt
│       ├── service/
│       │   ├── AlertProcessingService.kt
│       │   └── OpenCodeService.kt
│       └── rag/
│           ├── KnowledgeBaseService.kt
│           ├── TemplateDocument.kt
│           └── loader/TemplateLoader.kt
├── alert-bot-app/src/main/resources/
│   ├── application.yml
│   └── templates/
│       ├── cpu-overload.md
│       ├── memory-leak.md
│       ├── disk-full.md
│       ├── service-down.md
│       └── default.md
├── opencode.json                        # OpenCode MCP config
├── docker-compose.yml
├── .env.example
├── build.gradle.kts                     # Root build
├── settings.gradle.kts
└── PLAN.md                              # This file
```

---

## Implementation Phases

### Phase 1: Project Infrastructure
- Gradle multi-module setup with Kotlin DSL
- `mcp-server` and `alert-bot-app` modules
- Spring Boot 3.x + Spring AI 2.0.0 dependency management
- `.env.example`, `application.yml`, `opencode.json`

### Phase 2: MCP Server (YouGile Integration)
- Spring Boot app with `spring-ai-starter-mcp-server`
- **Tool `create_yougile_task`**: accepts `title`, `description`, `priority`, `columnId`
- **Tool `list_yougile_boards`**: fetches available boards
- **Tool `list_yougile_users`**: fetches users for assignment
- **Tool `list_yougile_columns`**: fetches columns for a board
- YouGile API client via `WebClient` with Bearer token auth
- Transport: **Streamable HTTP** on port `8081`
- Rate limiting: 45 req/min (under YouGile's 50 req/min)

### Phase 3: Alert Bot App — Telegram & Processing
- Telegram bot using `kotlin-telegram-bot` library
- Listen for incoming messages as alerts
- Model `Alert(id, text, source, timestamp, priority, category)`
- **`/testalert` command** — creates a demo alert with preset data, runs full pipeline
- **AlertProcessingService**: validate and structure the raw alert
- **OpenCodeService**: call OpenCode AI (zen provider) to classify alert, determine priority, suggest description

### Phase 4: RAG Knowledge Base (PGvector)
- PostgreSQL + pgvector extension in Docker Compose
- Spring AI pgvector-store for vector storage
- Embedding via OpenAI-compatible endpoint (zen.opencode.ai)
- Template documents in `resources/templates/` with YAML frontmatter
- `TemplateLoader` scans templates at startup, parses frontmatter, creates embeddings
- `KnowledgeBaseService` performs similarity search (top-3, threshold 0.7)
- Matched templates enrich the task description before creation

### Phase 5: MCP Client & Task Creation
- `spring-ai-starter-mcp-client` configured in `alert-bot-app`
- After OpenCode analysis + RAG enrichment → call MCP tool `create_yougile_task`
- Map alert fields → YouGile task fields
- Log result and send confirmation to Telegram chat

### Phase 6: Docker Compose
- `docker-compose.yml` with services: `postgres-pgvector`, `mcp-server`, `alert-bot-app`
- Shared Docker network, env vars from `.env`

---

## RAG Pipeline

```
Alert arrives → Text embedding via OpenCode (zen)
    │
    ▼
Vector search in PGvector (cosine similarity, top-3)
    │
    ▼
Retrieved templates + original alert
    │
    ▼
OpenCode AI generates final task title & description
    │
    ▼
MCP Client → create_yougile_task (title, description, columnId)
```

**Template example** (`templates/cpu-overload.md`):
```markdown
---
type: cpu
priority: critical
category: infrastructure
tags: [performance, database]
---
# CPU Overload

Server {host} CPU usage is at {value}% (threshold: {threshold}%).

## Impact
- Service {service} may become unresponsive
- Potential cascading failures to dependent services

## Recommended Actions
1. Connect to server: `ssh {host}`
2. Identify top processes: `top -b -n 1 | head -20`
3. Check recent deployments
```

---

## Configuration

```env
# Telegram
TELEGRAM_BOT_TOKEN=your_bot_token

# YouGile
YOUGILE_API_KEY=your_api_key_v2
YOUGILE_API_BASE_URL=https://yougile.com/api-v2
YOUGILE_DEFAULT_COLUMN_ID=column_uuid

# OpenCode (zen provider)
OPENCODE_API_KEY=your_opencode_key
OPENCODE_BASE_URL=https://zen.opencode.ai/v1
OPENCODE_MODEL=opencode-zen-1

# PostgreSQL (pgvector)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/alertbot
SPRING_DATASOURCE_USERNAME=alertbot
SPRING_DATASOURCE_PASSWORD=alertbot_pass
```

---

## YouGile API v2 Reference

| Endpoint                | Method | Purpose                    |
|-------------------------|--------|----------------------------|
| `/api-v2/auth/keys`     | POST   | Get API key (login+pass)   |
| `/api-v2/tasks`         | POST   | Create task                |
| `/api-v2/boards`        | GET    | List boards                |
| `/api-v2/boards/{id}`   | GET    | Get board details          |
| `/api-v2/users`         | GET    | List users                 |
| `/api-v2/columns`       | GET    | List columns               |

**Create Task Body:**
```json
{
  "title": "Alert: Database CPU > 90%",
  "description": "Critical alert from monitoring\nTimestamp: 2026-06-16T10:30:00Z\nSource: Prometheus\nDetails: CPU usage at 94% on db-prod-01",
  "columnId": "column-uuid"
}
```
