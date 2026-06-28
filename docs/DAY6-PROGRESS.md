# Smart Agriculture Advisor — Day 6 Progress

> **Project:** Smart Agriculture Advisor
> **Stack:** Java 21 · Spring Boot 4.0.6 · Spring Cloud · HTML · CSS · JavaScript
> **Date:** 2026-06-13
> **Approach:** AI-assisted development using Claude Code

---

## Table of Contents

1. [What Was Built Today](#1-what-was-built-today)
2. [Architecture](#2-architecture)
3. [Folder Structure](#3-folder-structure)
4. [Design Decisions](#4-design-decisions)
5. [UI Features](#5-ui-features)
6. [Query Types](#6-query-types)
7. [API Integration](#7-api-integration)
8. [How I Used Claude — Step-by-Step](#8-how-i-used-claude--step-by-step)
9. [How to Run](#9-how-to-run)
10. [What's Next](#10-whats-next)

---

## 1. What Was Built Today

| # | Task | Status |
|---|------|--------|
| 1 | Read CLAUDE.md and DAY1–DAY5 to understand full project state | ✅ Done |
| 2 | Identified that all 7 backend services were complete | ✅ Done |
| 3 | Decided to add Farmer Chat UI as a static page inside api-gateway | ✅ Done |
| 4 | Fixed ai-advisor gateway route timeout (10s → 60s) for Ollama response time | ✅ Done |
| 5 | Created `api-gateway/src/main/resources/static/index.html` — full chat UI | ✅ Done |
| 6 | Confirmed CORS already handled by api-gateway — no extra config needed | ✅ Done |
| 7 | Updated `CLAUDE.md` — added Frontend section with Farmer Chat UI details | ✅ Done |
| 8 | Updated memory file — project marked as feature-complete | ✅ Done |
| 9 | Created `docs/DAY6-PROGRESS.md` (this file) | ✅ Done |

---

## 2. Architecture

### Where the Farmer Chat UI fits

```
Farmer's Browser
      │
      ▼
http://localhost:8080/              ← static index.html served by api-gateway
      │
      │  POST /api/v1/advisor/advice  (same origin — no CORS)
      ▼
[ api-gateway ]                     ← port 8080 (Spring Cloud Gateway / WebFlux)
      │
      │  lb://ai-advisor-service
      ▼
[ ai-advisor-service ]              ← port 8084
      │
      ▼
[ Ollama — llama3.2:1b ]           ← local AI model
```

### Why static files inside api-gateway?

| Option | Pros | Cons |
|---|---|---|
| Static file in api-gateway (chosen) | No new service, no CORS, zero config | None for a demo |
| New `farmer-ui` Spring Boot service | Fully isolated | Needs CORS, extra port, extra service to run |
| Standalone HTML file (file://) | Zero servers | CORS blocks all API calls |

The api-gateway already serves as the single entry point on port 8080. Spring Boot's built-in `ResourceWebHandler` (WebFlux) automatically serves any file placed in `src/main/resources/static/` — no extra configuration needed.

---

## 3. Folder Structure

```
api-gateway/
└── src/
    └── main/
        ├── java/com/smartagriculture/apigateway/
        │   ├── ApiGatewayApplication.java
        │   ├── controller/
        │   │   └── FallbackController.java       ← circuit breaker fallbacks
        │   └── filter/
        │       └── LoggingFilter.java             ← request logging
        └── resources/
            ├── application.properties             ← updated: ai-advisor timeout 60s
            └── static/
                └── index.html                     ← NEW: Farmer Chat UI
```

---

## 4. Design Decisions

### Decision 1 — Static HTML/CSS/JS, no framework
A pure HTML/CSS/JS file was chosen over React/Vue/Angular because:
- No build step (no npm, no webpack)
- Loads instantly from the Spring Boot static folder
- Zero dependencies — works in any browser
- Easier to hand off and explain

### Decision 2 — Same-origin API calls (no CORS)
The chat page is served from `http://localhost:8080/` and calls `POST http://localhost:8080/api/v1/advisor/advice`. Since both are the **same origin**, the browser never makes a CORS preflight request. No `@CrossOrigin` annotation or CORS config was needed on `ai-advisor-service`.

### Decision 3 — 60-second route timeout for ai-advisor
The global gateway timeout was `10s`. Ollama running `llama3.2:1b` locally typically takes 15–40 seconds to generate a full response. A per-route override was added:
```properties
spring.cloud.gateway.server.webflux.routes[3].metadata.response-timeout=60000
```
Only the ai-advisor route is affected — all other routes keep the 10s global default.

### Decision 4 — Topic dropdown instead of auto-detection
The `queryType` enum is required by `ai-advisor-service`. Rather than trying to auto-detect the topic from the message (which would need another AI call), a simple dropdown lets the farmer choose the context. This makes the AI response more accurate and relevant.

---

## 5. UI Features

| Feature | Description |
|---|---|
| Welcome screen | Shown on first load with example question chips to get started |
| Example chips | 4 pre-written questions — click to fill the input box |
| Topic dropdown | 6 farming topics — sets the `queryType` sent to the AI |
| Chat bubbles | Farmer messages on the right (green), AI replies on the left (white) |
| Typing indicator | Animated 3-dot bounce shown while waiting for AI response |
| Timestamps | Each bubble shows the time the message was sent/received |
| Error display | API errors shown as a red bubble in the chat — not a popup |
| Auto-resize input | Textarea grows up to 3 lines as the farmer types |
| Enter to send | Enter sends, Shift+Enter adds a new line |
| Mobile-friendly | Responsive layout works on phones and tablets |
| Green theme | Agricultural colour scheme (#2e7d32) throughout |

---

## 6. Query Types

The topic dropdown maps human-readable labels to the `QueryType` enum values in `ai-advisor-service`:

| Dropdown Label | Enum Value | Use Case |
|---|---|---|
| General Question | `GENERAL` | Anything that does not fit other categories |
| Crop Recommendation | `CROP_RECOMMENDATION` | Which crop to grow for a given soil/climate |
| Pest Control | `PEST_CONTROL` | Identify and treat pests or diseases |
| Weather Alert | `WEATHER_ALERT` | Advice based on upcoming weather conditions |
| Planting Schedule | `PLANTING_SCHEDULE` | When to sow, transplant, or harvest |
| Irrigation Advice | `IRRIGATION_ADVICE` | When and how much to water |

---

## 7. API Integration

The chat UI makes a single API call:

**Endpoint:** `POST /api/v1/advisor/advice`
**Routed to:** `ai-advisor-service` via api-gateway load balancer

**Request sent by the browser:**
```json
{
  "question": "My rice leaves are turning yellow, what should I do?",
  "queryType": "PEST_CONTROL"
}
```

**Response displayed in the chat:**
```json
{
  "id": "7fa85f64-5717-4562-b3fc-2c963f66afa6",
  "question": "My rice leaves are turning yellow, what should I do?",
  "queryType": "PEST_CONTROL",
  "advice": "Yellow leaves on rice plants can be caused by several issues...",
  "generatedAt": "2026-06-13T10:00:00"
}
```

The UI reads `data.advice` and displays it as the AI's chat bubble.

**Error handling:**

| HTTP Status | What the UI shows |
|---|---|
| 503 | Circuit breaker fallback message from api-gateway |
| 400 | Validation error message from ai-advisor-service |
| Network error | "Something went wrong. Please try again." |

---

## 8. How I Used Claude — Step-by-Step

**Step 1 — Orient Claude with project state**

> *"Read my CLAUDE.md and memory file. Tell me if the project work is finished."*

Claude read CLAUDE.md, memory, and git status. Identified that all 7 backend services were committed/done and the api-gateway had uncommitted changes. Gave a clear status table.

---

**Step 2 — Decided on frontend type**

> *"I need a frontend where farmers can ask their problem."*

Claude read the `AiAdvisorController` and `AdvisorDto` to understand the exact request/response shape before designing the UI. Confirmed CORS was already covered by the gateway config.

---

**Step 3 — Identified the timeout bug before writing code**

Claude spotted that the global gateway timeout was 10s but Ollama responses take 15–40s. Fixed the `application.properties` before building the UI so it would not silently time out on the first real use.

---

**Step 4 — Built the full chat UI in one pass**

Claude created `api-gateway/src/main/resources/static/index.html` — a single self-contained file with all HTML, CSS, and JavaScript. No build tools, no dependencies, no extra configuration.

---

**Step 5 — Updated documentation**

> *"Update the MD file, add frontend, also update CLAUDE.md."*

Claude updated `CLAUDE.md` (added Frontend section), updated the memory file (marked project feature-complete), and created this `DAY6-PROGRESS.md`.

---

**What worked well with Claude:**
- Read the existing controller and DTO before writing any UI code — the API call in the HTML was correct first time
- Proactively caught the 10s timeout issue without being asked
- Chose the simplest deployment option (static file in api-gateway) that required zero new services and zero new config
- Followed the exact same DAY progress file format used in DAY1–DAY5

---

## 9. How to Run

### Prerequisites
- Java 21
- Maven 3.9+
- Ollama running locally with `llama3.2:1b` pulled
- `service-registry` and `ai-advisor-service` running

### Step 1 — Start service-registry
```bash
cd service-registry
mvn spring-boot:run
```
Eureka dashboard: http://localhost:8761

### Step 2 — Start ai-advisor-service
```bash
cd ai-advisor-service
mvn spring-boot:run
```
Verify on Eureka: `AI-ADVISOR-SERVICE` should appear at http://localhost:8761

### Step 3 — Start api-gateway
```bash
cd api-gateway
mvn spring-boot:run
```

### Step 4 — Open the chat UI
```
http://localhost:8080
```

Type a farming question, pick a topic, and press Enter.

---

## 10. What's Next

| Item | Status |
|---|---|
| `service-registry` | ✅ Complete |
| `farmer-service` | ✅ Complete |
| `crop-service` | ✅ Complete |
| `weather-service` | ✅ Complete |
| `ai-advisor-service` | ✅ Complete |
| `notification-service` | ✅ Complete |
| `api-gateway` | ✅ Complete |
| Farmer Chat UI | ✅ Complete |
| Docker + docker-compose (all 7 services) | 🔲 Optional — needed for live demo deployment |
| ngrok live demo | 🔲 Optional — expose localhost:8080 to the internet |
| Farmer ID / login on the chat UI | 🔲 Optional — pass farmerId to get personalised advice |

---

*Built with Java Spring Boot · Static HTML/CSS/JS · AI-assisted with Claude Code*