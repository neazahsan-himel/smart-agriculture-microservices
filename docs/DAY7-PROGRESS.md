# Smart Agriculture Advisor — Day 7 Progress

> **Project:** Smart Agriculture Advisor
> **Stack:** Java 21 · Spring Boot 4.0.6 · Spring Cloud 2025.1.0 · MySQL 8.x · Ollama (qwen3:8b)
> **Date:** 2026-08-31
> **Approach:** AI-assisted development using Claude Code

---

## Table of Contents

1. [What Was Built Today](#1-what-was-built-today)
2. [Scope Decision — Why Not All 11 MVP Core Features](#2-scope-decision--why-not-all-11-mvp-core-features)
3. [Architecture](#3-architecture)
4. [New Service: farm-asset-service](#4-new-service-farm-asset-service)
5. [ai-advisor-service — From Stateless to Stateful](#5-ai-advisor-service--from-stateless-to-stateful)
6. [The Confidence/Risk Tag Protocol](#6-the-confidencerisk-tag-protocol)
7. [Farmer Chat UI Changes](#7-farmer-chat-ui-changes)
8. [Live Verification — What Was Actually Tested](#8-live-verification--what-was-actually-tested)
9. [Bugs Found and Fixed](#9-bugs-found-and-fixed)
10. [Known Issues — Not Fixed (Out of Scope)](#10-known-issues--not-fixed-out-of-scope)
11. [How I Used Claude — Step-by-Step](#11-how-i-used-claude--step-by-step)
12. [How to Run](#12-how-to-run)
13. [What's Next](#13-whats-next)

---

## 1. What Was Built Today

| # | Task | Status |
|---|------|--------|
| 1 | Read `CLAUDE.md` roadmap + both Agro Agent PRD docs to scope "Phase 1" | ✅ Done |
| 2 | Surveyed existing services to find what was actually reusable vs. greenfield | ✅ Done |
| 3 | Scoped Phase 1 to the **MVP Core** feature tier, deferring voice (ASR/TTS) and image diagnosis (needs a provider decision) | ✅ Done |
| 4 | Built new `farm-asset-service` (port 8086) — per-farmer plots/ponds/fields | ✅ Done |
| 5 | Gave `ai-advisor-service` its own database and turned it from stateless Q&A into multi-turn conversations | ✅ Done |
| 6 | Added Farm Digital Memory, diagnostic cases, dynamic timeline, and a proactive follow-up scheduler | ✅ Done |
| 7 | Updated the Farmer Chat UI to drive real conversations instead of one-shot calls | ✅ Done |
| 8 | Spun up the full 8-service stack + MySQL + Ollama and ran a live end-to-end verification pass | ✅ Done |
| 9 | Found and fixed a real timeout/circuit-breaker config bug exposed by that testing | ✅ Done |
| 10 | Verified a full two-turn conversation through the actual browser UI (Chrome automation) | ✅ Done |

---

## 2. Scope Decision — Why Not All 11 MVP Core Features

`docs/prd/agro-agent-core-feature-matrix.md` tags 11 features as **MVP Core**. Two of them —
**Voice-First Agricultural Assistant** (Bangla ASR/TTS) and **Image-based Crop Diagnosis**
(computer vision) — need an external provider decision that hadn't been made yet (which speech
API, which vision-capable model). Building those blind would mean guessing at a provider and
likely redoing the work.

**Decision:** build the **text-based conversational, memory, safety, and follow-up backbone**
that the other 9 features depend on, and leave voice/vision as an explicit "Phase 1b" once a
provider is chosen. This was confirmed with the user via an explicit scope question before any
code was written.

| MVP Core Feature | Status |
|---|---|
| Human-like Conversational Onboarding | ✅ Built (multi-turn `Conversation`/`Message`) |
| Farm Digital Memory / Farm Brain | ✅ Built (`FarmEvent` log injected into every prompt) |
| Voice-First Agricultural Assistant | ⏸ Deferred — needs ASR/TTS provider decision |
| Image-based Crop Diagnosis | ⏸ Deferred — needs vision-model/provider decision |
| Smart Diagnostic Conversation | ✅ Built (model asks clarifying questions before diagnosing) |
| Safe Recommendation & Confidence System | ✅ Built (parsed confidence/risk/escalate tags) |
| Dynamic Farming Timeline | ✅ Built (`FarmTask`) |
| Best-Time Recommendation Engine | ✅ Built (weather-aware prompt instructions + due dates) |
| Proactive Follow-up Agent | ✅ Built (hourly scheduler → notification-service) |
| Weather-to-Decision Intelligence | ✅ Built (prompt turns forecast into explicit actions) |
| Outcome Tracking & Self-Learning | ✅ Built (`PATCH .../outcome` closes the loop) |

---

## 3. Architecture

```
Farmer's Browser
      │
      ▼
http://localhost:8080/                  ← Farmer Chat UI (unchanged host, new behavior)
      │
      │  POST /api/v1/advisor/conversations           (first message)
      │  POST /api/v1/advisor/conversations/{id}/messages   (follow-ups)
      ▼
[ api-gateway ] :8080
      │
      ├──lb://ai-advisor-service ──────────────► [ ai-advisor-service ] :8084 ─── ai_advisor_db (NEW)
      │                                                 │      │        │
      │                                                 │      │        └─ NotificationServiceClient (Feign)
      │                                                 │      └─ FarmerServiceClient / CropServiceClient / WeatherServiceClient
      │                                                 └─ Ollama (qwen3:8b) via OllamaApiClient
      │
      └──lb://farm-asset-service ──────────────► [ farm-asset-service ] :8086 (NEW) ─── farm_asset_db (NEW)
```

`ai-advisor-service` is now the "Agent brain": it owns conversation history, diagnostic cases,
farm memory, and the follow-up timeline, and it calls out to the other services (farmer, crop,
weather, notification) exactly as it already did — just with a lot more context assembled per turn.

---

## 4. New Service: farm-asset-service

A farmer's single flat profile (`farmer-service`) had no concept of individual plots or ponds —
everything in the PRD (Rice Plot 1, Rice Plot 2, Tilapia Pond, …) needs a `farmAssetId` to attach
memory, diagnoses, and tasks to. Rather than bolt this onto `farmer-service`, it was built as its
own service — consistent with this project's existing one-service-per-domain pattern
(`crop-service`, `weather-service`, `notification-service` are all separate from `farmer-service`
too).

| Field | Purpose |
|---|---|
| `farmerId` | cross-service reference (plain String, no FK — same pattern as `Notification.farmerId`) |
| `assetType` | `RICE_PLOT`, `POND`, `VEGETABLE_FIELD`, `ORCHARD`, `OTHER` |
| `label` | e.g. "Rice Plot 1" — what the farmer actually sees |
| `areaOrVolume` / `unit` | `HECTARE`, `DECIMAL`, `CUBIC_METER`, `OTHER` |
| `currentCropOrStock`, `stage` | free text for now — e.g. "BRRI dhan29" / "VEGETATIVE" |
| `status` | `ACTIVE`, `FALLOW`, `HARVESTED`, `INACTIVE` |

Standard endpoints at `/api/v1/farm-assets` (CRUD + paginated) plus a farmer-scoped
`GET /api/v1/farm-assets/farmer/{farmerId}`, following the exact same architecture rules as every
other service in this project (UUID id, soft delete, `@Version`, audit timestamps, DTO
request/response split, `GlobalExceptionHandler`).

---

## 5. ai-advisor-service — From Stateless to Stateful

Before today, `ai-advisor-service` had **no database** — every `/advice` call was one Ollama
round-trip with no memory of anything. It now has its own `ai_advisor_db` and five new entities:

| Entity | What it's for |
|---|---|
| `Conversation` | a thread — farmerId, optional farmAssetId, topic, open/closed |
| `Message` | every farmer/agent turn in a conversation, in order |
| `DiagnosticCase` | the "medical record" for one symptom: hypothesis, confidence, risk, follow-up date, outcome |
| `FarmEvent` | the append-only Farm Digital Memory log — every advice/diagnosis/outcome, per farmer+asset |
| `FarmTask` | the Dynamic Timeline — due dates, priority, linked back to the case that created it |

**The original `POST /api/v1/advisor/advice` endpoint was left completely untouched** — same
prompt, same single-shot behavior, zero risk to what was already working. All new behavior lives
behind new endpoints under `/api/v1/advisor/conversations`, `/diagnostic-cases`, `/farm-events`,
and `/farm-tasks`.

**Proactive follow-up:** an hourly `@Scheduled` job (`FollowUpScheduler`) finds diagnostic cases
and tasks that are due and pushes a notification through the existing `notification-service` —
reusing its own duplicate-PENDING-notification check as a natural spam guard, so a task that
isn't marked done doesn't flood the farmer every hour with duplicates.

---

## 6. The Confidence/Risk Tag Protocol

The PRD's "Safe Recommendation & Confidence System" needs the model to self-report how sure it
is and how risky the situation is. Asking a small local model (`qwen3:8b`) for strict JSON is
unreliable, so instead the system prompt requires a **fixed trailing block**:

```
---
CONFIDENCE: LOW|MEDIUM|HIGH
RISK: LOW|MEDIUM|HIGH|CRITICAL
FOLLOW_UP_DAYS: <integer, or NONE>
ESCALATE: YES|NO
```

`AdvisoryResponseParser` regex-extracts these four tags and strips the block from the
farmer-visible text — with safe defaults (`MEDIUM` confidence, `LOW` risk, no follow-up) if the
model forgets the block, so a malformed reply never breaks the request. This was verified live —
see [§8](#8-live-verification--what-was-actually-tested).

The same system prompt also instructs the model to **ask one clarifying question instead of
guessing** when the symptom description is too vague (Smart Diagnostic Conversation), and to
**turn weather data into an explicit timed action** instead of just restating the forecast
(Weather-to-Decision Intelligence).

---

## 7. Farmer Chat UI Changes

`api-gateway/src/main/resources/static/index.html` now drives the conversation endpoints instead
of the one-shot `/advice` call:

- A `conversationId` is kept in a page-level JS variable — the first message starts a
  conversation, every message after that continues it.
- A demo `farmerId` is generated per browser tab (`sessionStorage`) so conversations are
  attributable — a real login flow would replace this.
- Risk/confidence badges (green/amber/red) render next to each agent reply when a diagnostic
  case is present.
- Everything else (topic dropdown, example chips, typing indicator) is unchanged.

---

## 8. Live Verification — What Was Actually Tested

Unlike most days, this session didn't stop at "it compiles" — the full stack was actually started
(MySQL, Ollama, all 7 services + gateway) and driven end-to-end:

| Check | Result |
|---|---|
| Farm asset created via gateway, retrievable by farmer | ✅ Pass |
| Turn 1 with a vague symptom → model asks a clarifying question, `LOW` confidence, stays `OPEN` | ✅ Pass |
| Turn 2 with the missing details → real diagnosis, `MEDIUM` confidence/risk, `AWAITING_FOLLOWUP`, 5-day follow-up | ✅ Pass |
| Tag block cleanly parsed and stripped from visible text every time | ✅ Pass |
| `FarmEvent` logged for both turns; `FarmTask` created with matching due date | ✅ Pass |
| Forced a case/task overdue → scheduler sent 2 real notifications, case flipped to `FOLLOWUP_SENT` | ✅ Pass |
| Outcome recorded (`IMPROVED`) → case `RESOLVED`, `OUTCOME` farm event logged | ✅ Pass |
| Real two-turn conversation in an actual Chrome browser (via Claude in Chrome automation) | ✅ Pass |

The browser test is the strongest proof of multi-turn memory: turn 2 asked *"which of **those
two varieties** matures faster?"* with no crop names repeated — the model could only answer
correctly ("Chinigura matures faster than Basmati") because the full conversation history was
being passed into the prompt.

---

## 9. Bugs Found and Fixed

**Timeout/circuit-breaker config was sized for a fast model.** `qwen3:8b` is a "thinking" model,
and this machine's CPU-only inference is slow — a trivial prompt took ~50 seconds, and a real
conversation turn (with farmer profile, crop catalog, weather, farm memory, and history in the
prompt) took up to ~4.5 minutes. The existing 60-second gateway route timeout and 65-second
circuit-breaker `TimeLimiter` (both sized when the docs assumed "Ollama can be slow" meant
15–40s) were tripping before Ollama could finish. Fixed by raising all three layers to match:

- `AppConfig.restTemplate()` read timeout: 120s → 600s
- Gateway route `[3]` response-timeout: 60s → 600s
- `GatewayCircuitBreakerConfig`'s `ai-advisor-service` `TimeLimiter`/`slowCallDurationThreshold`: 65s → 605s

**Recommendation:** multi-minute replies aren't viable for a real interactive chat experience.
For production, either switch to a smaller/non-thinking model (`llama3.2:1b` is already pulled
locally) or add GPU acceleration.

---

## 10. Known Issues — Not Fixed (Out of Scope)

- **`createdAt`/`updatedAt` come back `null` on `POST` create responses.** Confirmed on
  `farmer-service` (untouched by this session), so it's a pre-existing pattern across every
  service's create-method (`@Transactional` hasn't flushed yet when the response DTO is built),
  not something Phase 1 introduced. Not fixed because it touches already-shipped code outside
  this plan's scope — a `GET` immediately after `POST` always shows the correct value.
- This session's dev machine has the JVM and MySQL both reporting `+06:00`, while the Git Bash
  shell's own `date` command reports `BST` (`+01:00`) — a shell-environment quirk, not an
  application bug. Worth knowing if you ever manually write timestamps via a raw SQL client:
  use `UTC_TIMESTAMP()`, not `NOW()`, to match what the app itself stores (everything is stored
  as UTC per `serverTimezone=UTC` in the JDBC URL).

---

## 11. How I Used Claude — Step-by-Step

**Step 1 — Scoped "Phase 1"**

> *"start phase 1"*

The roadmap in `CLAUDE.md` didn't define a "Phase 1" — it uses PRD tiers (MVP Core → MVP → …).
Claude asked which tier was meant rather than guessing, then surveyed the existing codebase
before proposing anything.

**Step 2 — Planned before writing code**

Given the size (new service + a service going from stateless to stateful + scheduler + UI
changes), Claude entered plan mode, asked one clarifying question about voice/vision scope, and
wrote a concrete plan file naming exact entities, endpoints, and file paths before touching any
code.

**Step 3 — Built in parallel**

The mechanical, well-specified `farm-asset-service` (copy the existing `farmer-service` pattern)
was handed to a background sub-agent while Claude built the more nuanced `ai-advisor-service`
conversation/memory/scheduler logic directly — both landed and compiled cleanly.

**Step 4 — Verification was requested explicitly and run for real**

> *"Please walk me through spinning up the MySQL, Ollama, Eureka, and all required services,
> then run the full Verification checklist and drive one complete end-to-end conversation."*

Claude didn't just claim success — it started the actual stack, hit two real failures along the
way (Maven offline-mode plugin resolution; the timeout/circuit-breaker mismatch above), diagnosed
each from logs rather than guessing, fixed the timeout config, and re-verified. It also caught its
own testing mistake (writing a past timestamp with server-local `NOW()` instead of
`UTC_TIMESTAMP()`) by cross-checking raw DB values against API responses instead of assuming the
first weird result was an app bug.

**Step 5 — Proved it with a real browser, not just curl**

Rather than stopping at API-level checks, Claude used Chrome browser automation to drive an
actual two-turn conversation through the deployed UI and confirmed the second answer only made
sense with conversation memory intact.

---

## 12. How to Run

### Prerequisites
- Java 21, Maven 3.9+
- MySQL 8.x running with `root`/`root` credentials (or update each service's
  `application.properties`)
- Ollama running locally with a model pulled (`qwen3:8b` configured; `llama3.2:1b` is faster if
  you want quicker local testing)

### Step 1 — Create the two new databases (one-time)
```sql
CREATE DATABASE IF NOT EXISTS farm_asset_db;
CREATE DATABASE IF NOT EXISTS ai_advisor_db;
```

### Step 2 — Start every service, in this order
```bash
cd service-registry      && mvn spring-boot:run   # :8761
cd farmer-service        && mvn spring-boot:run   # :8081
cd crop-service          && mvn spring-boot:run   # :8082
cd weather-service       && mvn spring-boot:run   # :8083
cd farm-asset-service    && mvn spring-boot:run   # :8086  (NEW)
cd notification-service  && mvn spring-boot:run   # :8085
cd ai-advisor-service    && mvn spring-boot:run   # :8084  (now stateful)
cd api-gateway           && mvn spring-boot:run   # :8080
```
Check `http://localhost:8761` — all should show `UP` before starting `api-gateway`.

### Step 3 — Open the chat UI
```
http://localhost:8080
```
Ask a vague symptom question first — the agent should ask a clarifying question before giving
advice. Answer it, and watch for the risk/confidence badges on the reply.

**Heads up:** with `qwen3:8b` on CPU-only inference, expect replies to take **1–5 minutes each**
once a conversation has some history. This is expected given the current model/hardware — see
[§9](#9-bugs-found-and-fixed).

---

## 13. What's Next

| Item | Status |
|---|---|
| `service-registry`, `farmer-service`, `crop-service`, `weather-service`, `notification-service`, `api-gateway` | ✅ Complete |
| Farmer Chat UI | ✅ Complete (now multi-turn) |
| `farm-asset-service` | ✅ Complete |
| `ai-advisor-service` — conversation/memory/confidence/timeline/follow-up backbone | ✅ Complete |
| Voice-First Assistant (Bangla ASR/TTS) | 🔲 Phase 1b — needs a provider decision |
| Image-based Crop Diagnosis (computer vision) | 🔲 Phase 1b — needs a vision-model/provider decision |
| Master Agent + Specialist Agents orchestration | 🔲 Phase 2 (PRD tier) |
| Real Human Expert Escalation routing (currently just a status flag) | 🔲 Not yet built |
| Switch to a faster/non-thinking model or add GPU for interactive latency | 🔲 Recommended before any real farmer-facing demo |
| Fix `createdAt`/`updatedAt: null` on `POST` create responses (all services) | 🔲 Pre-existing, low priority |

---

*Built with Java Spring Boot · MySQL · Ollama · AI-assisted with Claude Code*
