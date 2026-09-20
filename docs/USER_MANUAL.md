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
| **weather-service** | 8083 | Recorded weather conditions (temperature, rainfall, condition) used as context for advice. |
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

1. Farmer picks a topic from a dropdown (General, Crop Recommendation, Pest Control, Weather
   Alert, Planting Schedule, Irrigation Advice) — or just starts typing.
2. Farmer types a question, or attaches a photo (e.g. a leaf with spots), and hits send.
3. A typing indicator shows while the AI thinks (this can legitimately take a while — see
   the timeout note below).
4. The advisor replies, and the conversation continues — every message is remembered, so the
   farmer can say "it's still not better" three days later and the advisor has full context.

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
   - recent weather (`weather-service`)
   - up to 5 recent "farm memory" events for this farmer/asset — past advice, past
     outcomes — so the advisor doesn't repeat itself or contradict earlier guidance
   - the full message history of this conversation
3. **Builds one structured prompt** out of all of that and sends it to Ollama — the text
   model (`qwen3:8b`) for a normal question, or the vision model (`llava:7b`) if a photo was
   attached.
4. **Parses the reply** for three things beyond the visible answer:
   - a **confidence level** (how sure the model is)
   - a **risk level** (LOW/MEDIUM/HIGH/CRITICAL — how bad it is if the advice is wrong)
   - whether it needs a **follow-up in N days**, or should be **escalated**
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

---

## 6. Running it locally (for a live demo)

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
6. Open `http://localhost:8080/` — that's the whole demo.
7. Optional: `scripts/seed-data.sql` has ready-made sample farmers (e.g. "John Rahman",
   Rajshahi; "Rahim Uddin", Manikganj) so the chat has something realistic to reference
   instead of an empty database.

---

## 7. What's real today vs. what's still a plan

This matters when explaining the project so nobody overclaims what exists:

**Actually built and working** (everything above): farmer/crop/weather/farm-asset/
notification CRUD, the conversational advisor with photo-based diagnosis, diagnostic case
tracking with confidence/risk, automatic follow-up task scheduling, and a working chat UI.

**Documented as vision, not yet built** — captured in `docs/prd/`:
- Voice input/output (today it's text + photo only, no speech)
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
