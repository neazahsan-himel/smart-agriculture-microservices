# Smart Agriculture Advisor — User Manual

> A plain-language explainer for the project: what it is, how the pieces fit together, and
> how to walk someone through a live demo. Written from the actual current codebase — not
> the future vision (that lives in `docs/prd/`, see the last section).

---

## 1. What this project is, in one sentence

A chat assistant for farmers in Bangladesh: a farmer describes a problem (in text, or with a
photo of a sick plant), and an AI advisor — built on top of several small backend
services — answers with practical advice, remembers the conversation, and follows up later
to see if the problem was solved.

---

## 2. The big picture

The system is a set of small, independent **microservices** (separate Spring Boot
applications, each with its own database) that all sit behind one shared front door.

```
                        ┌────────────────────────┐
   Farmer's browser ───▶│   api-gateway  (8080)   │◀── serves the Chat UI (index.html)
                        └───────────┬─────────────┘
                                    │  routes each request by URL path
        ┌───────────┬───────────┬──┴────────┬─────────────┬──────────────────┐
        ▼           ▼           ▼            ▼             ▼                  ▼
  farmer-service crop-service weather-service farm-asset-service notification-service ai-advisor-service
     (8081)        (8082)        (8083)          (8086)              (8085)             (8084)
   farmer_db     crop_db      weather_db      farm_asset_db     notification_db      ai_advisor_db
                                                                                          │
                                                                                          ▼
                                                                                   Ollama (local LLM)
                                                                              qwen3:8b (text) /
                                                                              llava:7b (vision)

  All services register themselves with:  service-registry (Eureka, port 8761)
```

**Why split it up this way?** Each service owns one kind of data (farmers, crops, weather,
farm assets, notifications, AI conversations) and can be built, tested, and restarted
independently. The `api-gateway` is the *only* thing farmers' browsers ever talk to — it
looks at the URL and forwards the request to the right service. This also means every
request gets circuit-breaker protection: if one service is down or slow, the gateway returns
a graceful fallback instead of hanging the whole app.

---

## 3. The services, in plain terms

| Service | Port | What it's responsible for |
|---|---|---|
| **service-registry** | 8761 | The phone book. Every other service registers here so they can find each other by name instead of hardcoded addresses. |
| **farmer-service** | 8081 | Farmer profiles: name, phone, location, farm size, soil type, irrigation method, language preference. |
| **crop-service** | 8082 | A reference catalog of crops (name, variety, ideal soil/temperature/rainfall, growing duration, typical yield) — the AI looks this up when giving advice. |
| **farm-asset-service** | 8086 | Each individual plot/pond a farmer owns — e.g. "Rice Plot 1", "Tilapia Pond" — with its own area, current crop/stock, and stage. |
| **weather-service** | 8083 | Weather records, plus an hourly **forecast for any location**, pulled from Open-Meteo (free, no API key) and cached for 3 hours. |
| **notification-service** | 8085 | Alerts to farmers (weather alert, crop advisory, pest warning, irrigation reminder), each with a priority (LOW → CRITICAL) and channel (SMS/push/in-app/email). |
| **ai-advisor-service** | 8084 | The brain. Runs the conversation with the farmer, calls the local LLM, and tracks diagnostic cases and follow-ups (details in §5). |
| **api-gateway** | 8080 | The single public entry point. Routes requests, applies circuit breakers, and serves the Chat UI. |

Every service follows the same internal design (documented fully in `CLAUDE.md`): soft
delete instead of hard delete, optimistic locking (`@Version`) to avoid lost updates,
created/updated timestamps on everything, and a consistent `Request`/`Response` DTO split so
internal fields (like `deleted`) never leak into API responses.

---

## 4. How a farmer actually uses it

The whole product is one page: `http://localhost:8080/` (a static Chat UI served directly by
the api-gateway — no separate frontend server, no CORS issues since it's same-origin).

1. **Who is farming?** On the first visit a start screen asks the farmer to pick their name
   from the registered farmers, or register (name, phone, region, main crop, and location —
   with a "📍 Use my current location" button). The choice is remembered in the browser; the
   header shows "👨‍🌾 Name · Switch". A ⚠️ next to the name means no location is saved, so
   weather and best-time advice can't work. "Continue as guest" chats without a profile.
   (There is no real login yet; `?farmerId=<id>` in the URL overrides the choice.)
2. Farmer picks a topic from a dropdown (General, Crop Recommendation, Pest Control, Weather
   Alert, Planting Schedule, Irrigation Advice) — or just starts typing.
3. Farmer types a question, attaches a photo (e.g. a leaf with spots), or records a voice
   message (🎤), and hits send.
4. A typing indicator shows while the AI thinks (this can legitimately take a while — see
   the timeout note below).
5. The advisor replies, with Risk/Confidence badges and an optional ▶ voice reply. The
   conversation continues — every message is remembered, so the farmer can say "it's still
   not better" three days later and the advisor has full context.
6. **Task cards.** When the advice involves weather-sensitive work (spray, fertilise,
   irrigate, harvest, sow), a card appears under the topic bar: green with "Best time: Mon
   2 Nov, 07:00", or amber when there's no safe time / no forecast (the card says why).
   **↻ Refresh** re-checks the latest forecast; **✓ Done** / **Skip** close the task. Cards
   the latest reply created or moved flash briefly.
7. **🔔 Notifications.** The bell in the header shows an unread count of in-app alerts:
   weather plans ("Weather plan: spray on Mon 2 Nov 06:00", "Good time to spray from…") and
   follow-up reminders. Opening the bell lists them; clicking one marks it read. It checks
   for new alerts every minute.
8. **📅 This week.** The calendar button opens the farmer's week: overdue work in red, then
   one card per day (today highlighted) with the weather (first ~3 days), planned work at its
   best time, follow-up checks ("Check on: yellow leaves…") with their risk level, and ✓ / Skip
   buttons; recent farm activity at the bottom.

**Why can a reply take so long?** The AI model (`qwen3:8b`) is a "thinking" model, running
on CPU, and the prompt sent to it includes the farmer's profile, crop catalog, recent
weather, farm history, and the whole conversation so far. The gateway route for the advisor
has its timeout raised to **10 minutes** specifically to accommodate this (see
`api-gateway/.../application.properties`, route `ai-advisor-service`).

---

## 5. What actually happens behind the chat (the interesting part)

This is the part worth explaining carefully, because it's more sophisticated than "send a
message to an LLM and show the reply."

Every time a farmer sends a message (`POST /api/v1/advisor/conversations` to start, or
`POST /api/v1/advisor/conversations/{id}/messages` to continue), `ai-advisor-service` does
this, per turn:

1. **Saves the farmer's message** (and photo, if any) to the conversation history.
2. **Gathers context** by calling the other services:
   - the farmer's profile (`farmer-service`)
   - the crop catalog (`crop-service`)
   - the next 48 hours of weather **for the farmer's own location** (`weather-service`,
     from the latitude/longitude on their profile), condensed into 3-hour blocks
   - up to 5 recent "farm memory" events for this farmer/asset — past advice, past
     outcomes — so the advisor doesn't repeat itself or contradict earlier guidance
   - the full message history of this conversation
3. **Builds one structured prompt** out of all of that and sends it to Ollama — the text
   model (`qwen3:8b`) for a normal question, or the vision model (`llava:7b`) if a photo was
   attached.
4. **Parses the reply** for four things beyond the visible answer:
   - a **confidence level** (how sure the model is)
   - a **risk level** (LOW/MEDIUM/HIGH/CRITICAL — how bad it is if the advice is wrong)
   - whether it needs a **follow-up in N days**, or should be **escalated**
   - whether the advice involves **weather-sensitive work** (spray, fertilise, irrigate,
     harvest, sow) and in how many days — see "Best time to do it" below
5. **Opens or updates a "diagnostic case"** for the conversation — a persistent record of
   "what's the problem, what's the working hypothesis, what did we recommend, what's the
   confidence/risk, is it resolved yet."
6. If a follow-up is needed, it **automatically schedules a farm task** (`FOLLOW_UP` type,
   due date = today + N days, priority derived from the risk level). A background scheduler
   (`FollowUpScheduler`) checks for these and can prompt the farmer again later — e.g. "3
   days ago you had a pest problem — how does it look now?"
7. Every advice reply and outcome is also logged as a **farm event**, building up the
   farmer's history over time.

So the mental model to give someone is: **it's not a single Q&A bot — it's a case-tracking
system with an LLM doing the reasoning at each step**, and every case has a lifecycle:
`OPEN → AWAITING_FOLLOWUP → RESOLVED` (or `ESCALATED` if confidence is too low / risk too
high).

When a farmer later confirms whether the problem improved, that's recorded via
`PATCH /api/v1/advisor/diagnostic-cases/{id}/outcome` — this closes the loop and is what the
project's roadmap calls "outcome tracking."

### Best time to do it (Weather-to-Decision)

Some farm work only makes sense in the right weather: spraying just before rain wastes the
pesticide, and fertiliser washes off in a downpour. For these jobs the system doesn't just
show a forecast; it **picks the time**.

- A weather-sensitive task is created either by the farmer/app
  (`POST /api/v1/advisor/farm-tasks` with `SPRAY`, `FERTILIZE`, `IRRIGATE`, `HARVEST` or
  `SOW` and a planned date) or **automatically from the chat**, when the advisor recommends
  one of those jobs.
- A rule-based engine (`BestTimeEvaluator`) scans the hourly forecast around the planned
  date and picks the **earliest safe hour**. Examples: spraying needs 6–10am or 4–6pm, wind
  ≤ 15 km/h and no rain for 6 hours after; fertiliser must not get 10 mm+ of rain in the next
  24 hours. It explains its choice ("Earlier times were skipped: 4.2 mm rain expected…").
- In the chat, that verdict is added to the advisor's reply as a **"Forecast check:"** line.
  The rule-based time is the one to trust: the LLM's own timing in the prose can be off
  (see `docs/DAY11-PROGRESS.md`).
- Every hour, `WeatherTaskScheduler` re-checks pending tasks against the latest forecast and
  sends a **WEATHER_ALERT** notification only when something changes: the best time moves,
  no safe time is left, or the chosen time is about to start ("Good time to spray from…").
- `GET /api/v1/advisor/farm-tasks/{id}/best-time` re-checks a task on demand.

The engine is deliberately rules, not AI: the same forecast always gives the same answer,
and it can say exactly why.

---

## 6. Running it locally (for a live demo)

### Option 1: everything in Docker (one command)

Needs Docker Desktop and **Ollama running on the host** with `qwen3:8b` and `llava:7b` pulled.
Ollama stays outside Docker on purpose (GPU access, models already downloaded); the containers
reach it at `host.docker.internal:11434`.

```bash
docker compose up -d --build      # first build takes a few minutes
```

Then open `http://localhost:8080/?farmerId=<id>` (Eureka dashboard: `http://localhost:8761`).

| Published port | What |
|---|---|
| 8080 | api-gateway + Farmer Chat UI |
| 8761 | Eureka dashboard |
| 3307 | MySQL (`root` / `root`), so it doesn't clash with a host MySQL on 3306 |
| 9000 / 5002 | Whisper ASR / Piper TTS |

- The other services are **not** published; they talk to each other inside the compose network.
  That also avoids clashes with anything else on the host using 8081–8086.
- The Docker MySQL starts **empty** (its own volume). Databases are created automatically; tables
  appear when the services start. To load the sample farmers, once the services are up:
  `docker compose exec -T mysql mysql -uroot -proot < scripts/seed-data.sql`
- Containers run in `Asia/Dhaka` time, because the best-time rules ("spray 06–10") use the JVM
  clock. Change it with `APP_TIMEZONE=... docker compose up -d`.
- Stop with `docker compose down`. Add `-v` to also delete the database.
- Images are built with tests skipped; run tests with `mvnw test` outside Docker.

### Option 2: each service with Maven

Startup order matters because everything depends on `service-registry` and, in turn, on
MySQL being reachable:

1. Create the databases (MySQL, one per service): `farmer_db`, `crop_db`, `weather_db`,
   `farm_asset_db`, `notification_db`, `ai_advisor_db`. Each service's
   `spring.jpa.hibernate.ddl-auto=update` creates/updates tables automatically after that.
2. Start **Ollama** locally and make sure `qwen3:8b` and `llava:7b` are pulled
   (`ollama.api.base-url=http://localhost:11434` in `ai-advisor-service`'s config) — the
   advisor won't be able to answer without this running.
3. Start `service-registry` first (port 8761) — everything else registers with it.
4. Start the remaining services in any order: `farmer-service`, `crop-service`,
   `weather-service`, `farm-asset-service`, `notification-service`, `ai-advisor-service`.
5. Start `api-gateway` last (port 8080).
6. Open `http://localhost:8080/` — that's the whole demo. Pick or register a farmer on the
   start screen (one with a location, for the weather features).
7. Forecasts need **internet access** (to `api.open-meteo.com`) and a farmer profile with
   `latitude`/`longitude` set. Without either, the advisor falls back to whatever weather
   records are stored and the best-time engine reports "no forecast available".
8. Optional: `scripts/seed-data.sql` has ready-made sample farmers (e.g. "John Rahman",
   Rajshahi; "Rahim Uddin", Manikganj) so the chat has something realistic to reference
   instead of an empty database.

---

## 7. What's real today vs. what's still a plan

This matters when explaining the project so nobody overclaims what exists:

**Actually built and working** (everything above): farmer/crop/weather/farm-asset/
notification CRUD, the conversational advisor with photo-based diagnosis and voice
input/output (Bangla speech via local Whisper/Piper or Google), diagnostic case tracking with
confidence/risk, automatic follow-up task scheduling, location-specific forecasts with
best-time recommendations and weather alerts, and a working chat UI with farmer selection,
weather task cards and an in-app notification bell. The whole stack also runs with one
`docker compose up` (§6).

**Not built yet:**
- Real login / authentication (the start screen just remembers a chosen farmer in the browser)
- Choosing a specific plot/pond (farm asset) from the chat UI

- Advice grounded in official Bangladeshi data: `knowledge-service` exists (tables, CSV import,
  citations) but holds no real data yet; plan in `docs/plans/rice-knowledge-pilot.md`

**Documented as vision, not yet built** — captured in `docs/prd/`:
- Human-expert escalation actually routing to a person
- Rice/Vegetable/Aquaculture-specific reasoning modules
- Market price intelligence, financial/cash-flow advice
- Multi-agent orchestration (one "master" agent plus specialists per plot/pond)

If you're demoing this to someone, the honest framing is: *"the infrastructure and the core
conversational-diagnosis loop are real and running against a local LLM; the fuller
voice-first, multi-domain 'Agro Agent' vision is a documented roadmap, not shipped yet."*

---

## 8. Where to look for more detail

- `CLAUDE.md` (repo root) — the architecture rules every service follows.
- `ai-advisor-service/AI-ADVISOR-SERVICE.md` — note: this describes an older, stateless,
  Gemini-based design. The service has since moved to a stateful, conversation-tracking
  design using Ollama, as described in §5 above — treat that file as historical.
- `docs/prd/agro-agent-core-feature-matrix.md` — the full 49-feature product vision.
- `docs/prd/agro-agent-karim-saheb-capability-matrix.md` — a concrete persona
  (a rice/pond/vegetable farmer) with 31 real-world scenarios the vision is designed around.
