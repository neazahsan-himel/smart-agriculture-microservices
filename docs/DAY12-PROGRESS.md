# Smart Agriculture Advisor — Day 12 Progress

> **Project:** Smart Agriculture Advisor
> **Stack:** Java 21 · Spring Boot 4.0.6 · Spring Cloud 2025.1.0 · MySQL 8.x · Ollama · Docker
> **Date:** 2026-11-01
> **Approach:** AI-assisted development using Claude Code

---

## 1. What Was Built

Day 11 shipped the Weather-to-Decision and Best-Time engines, but a farmer could only reach them
through the API: tasks, alerts and the farmer's identity were all invisible in the Chat UI. Day 12
makes them usable from the browser and makes the whole stack start with one command.

| # | Feature | Where | Backend changes |
|---|---|---|---|
| 1 | Weather task cards in the Chat UI | `api-gateway/.../static/index.html` | None |
| 2 | Notification bell (in-app alerts) | `index.html` | None |
| 3 | Farmer selection start screen | `index.html` | None |
| 4 | Full Docker setup | `docker-compose.yml`, `docker/spring-service/Dockerfile`, `docker/mysql/init/`, `.dockerignore` | None (config via env vars) |

The follow-up reminder fix from earlier the same day is logged in `DAY11-PROGRESS.md` §4.6.

---

## 2. Weather Task Cards

A horizontal strip under the topic bar, one card per **pending weather-dependent** task.

| Card part | Source |
|---|---|
| Title with icon (🧴 Spray, 🌱 Fertilise, 💧 Irrigate, 🌾 Harvest, 🌰 Sow) | `taskType` |
| "Best time: Mon 2 Nov, 07:00" (green) or "No recommended time yet" (amber) | `recommendedStartAt` |
| Planned date · last checked | `dueDate`, `weatherCheckedAt` |
| Reason text | `weatherAdvice` |
| **↻ Refresh** / **✓ Done** / **Skip** | `GET /farm-tasks/{id}/best-time`, `PATCH /farm-tasks/{id}/status` |

- Loaded via `GET /api/v1/advisor/farm-tasks?farmerId=…`, filtered client-side to
  `weatherDependent && PENDING`.
- Reloaded after every advisor reply. A card whose due date, best time or advice changed (i.e. the
  reply's `TASK:` tag created or moved it) flashes green.
- Amber covers three cases (no safe window, nothing needed, no forecast); the advice text says
  which, so the UI doesn't parse it.

---

## 3. Notification Bell

🔔 in the header with an unread count; a dropdown lists the farmer's `IN_APP` notifications
(newest first, icon per type, priority colour on the left edge, "5 min ago").

**Lifecycle mapping.** notification-service enforces `PENDING → SENT → READ` and rejects READ from
PENDING (`400 "Only SENT notifications can be marked as READ"`). Nothing marked in-app alerts SENT,
because the schedulers create them PENDING and there is no delivery worker for IN_APP. Rather than
loosen the backend rule, the UI maps the lifecycle onto what the farmer does:

| Farmer action | Transition | Meaning |
|---|---|---|
| Opens the bell | shown `PENDING → SENT` | delivered to the screen |
| Clicks an alert / "Mark all read" | `→ READ` (via SENT if still PENDING) | acknowledged |

Unread = anything not READ. Polls every 60 s while closed (schedulers run hourly).

---

## 4. Farmer Selection

Until now the UI used a random `demo-<uuid>` per tab. That ID has no profile and no location, so
Day 11's weather features could never work from the UI.

- **Start screen** ("Who is farming today?") on first visit:
  - *I'm registered*: `GET /api/v1/farmers?size=100&sortBy=name`, with 📍 / "no location" per farmer.
  - *New farmer*: name, phone, region, country, main crop, lat/lon, plus **📍 Use my current
    location** (browser geolocation). `POST /api/v1/farmers`; server `fieldErrors` are shown
    under each field, and a 409 duplicate phone shows the server's message.
  - *Continue as guest*: keeps the old `demo-` behaviour.
- The choice goes in `localStorage`; choosing reloads the page so the conversation, cards and bell
  start fresh. `?farmerId=<id>` still overrides it.
- **Header chip** "👨‍🌾 Name · Switch", with ⚠️ and a tooltip when the farmer has no location.
- If the stored farmer was deleted (404), the stored ID is cleared and the start screen reopens.

Not a login: anyone can pick any farmer. Real authentication is future work.

---

## 5. Docker: One-Command Stack

```
docker compose up -d --build   →   http://localhost:8080/
```

| Decision | Why |
|---|---|
| One shared Dockerfile, `SERVICE` build arg, repo-root context | 8 identical modules; one place to change |
| Multi-stage build (Maven → JRE) with a BuildKit `.m2` cache mount | Small runtime image; dependencies download once for all services |
| Tests skipped in the image build | `@SpringBootTest` context tests need MySQL |
| Docker addresses as env vars, **no `application.properties` changes** | `mvnw spring-boot:run` keeps working unchanged |
| Eureka URL via `SPRING_APPLICATION_JSON` | Env-var relaxed binding lowercases map keys (`defaultzone`); JSON keeps the exact `defaultZone` key |
| `-Duser.timezone=Asia/Dhaka` (`APP_TIMEZONE`) | Containers default to UTC; the best-time rules ("spray 06–10") use the JVM clock and would be 6 h off |
| Ollama stays on the host (`host.docker.internal:11434`) | GPU access, models already pulled |
| MySQL on 3307 with its own volume; `01-create-databases.sql` | No clash with a host MySQL; tables still come from `ddl-auto=update` |
| MySQL healthcheck pings over TCP | During first-run init the temporary server has networking off, so services wait for the real one |
| Only 8080, 8761, 3307, 9000, 5002 published | Services talk inside the compose network; avoids clashes with other apps on 8081–8086 |
| Heap: 384m per service, 768m for ai-advisor | Base64 photos/audio in prompts |

The seed script can't run at MySQL init (the tables don't exist until the services start), so it
is loaded afterwards: `docker compose exec -T mysql mysql -uroot -proot < scripts/seed-data.sql`.

---

## 6. Verification

| # | Check | Result |
|---|---|---|
| 1 | Task list / best-time / status endpoints through the gateway | ✅ |
| 2 | Best time with a reachable farmer-service (Dhaka test farmer, HARVEST planned Mon 10:00, created Sun 20:00) | ✅ "Best time to harvest: Mon 2 Nov 07:00", the first allowed harvest hour |
| 3 | Old SPRAY task planned 30 Oct | ✅ "No safe time to spray before Sun 1 Nov 21:51" (search window ends 48 h after the plan) |
| 4 | Notification lifecycle as the bell drives it | ✅ READ from PENDING → 400; SENT → READ → 200 |
| 5 | Farmer list sorted by name, unknown id → 404, invalid registration → 400 with `fieldErrors` keys matching the form | ✅ (no farmer created) |
| 6 | `docker compose config` | ✅ valid; ai-advisor gets the 768m heap override |
| 7 | `docker compose build` for all 8 services | ✅ images 569–692 MB |
| 8 | Cards, bell and start screen rendered in a browser | 🔲 **Not done**: the Chrome extension wasn't connected, and the running gateway served the pre-change page |
| 9 | `docker compose up` end to end | 🔲 **Not done**: ports 8080/8761 were held by locally started services |

The new JavaScript has not been executed or syntax-checked (no Node.js on this machine). Checks
8 and 9 are the first thing to do next session.

---

## 7. Found Along the Way

| Finding | Status |
|---|---|
| Port 8081 on the dev machine was held by an unrelated app (`VisaRagServiceApplication`); every "farmer" call returned 500, so all forecasts said "No forecast available" | Worked around by running farmer-service on 8087 (Eureka resolves by name). Not a code issue; Docker avoids it because 8081 isn't published |
| `CLAUDE.md` said the UI calls `/advisor/advice` and the advisor timeout is 60 s | Fixed: conversations endpoints, 10 min |
| `GET /api/v1/farmers` returns farmers without location (legacy test rows); they show "no location" in the picker | Expected; data, not code |

---

## 7b. Later the Same Day: Rice Knowledge Pilot Started

After a discussion about real Bangladeshi data, a pilot plan was written to ground rice advice in
official sources (`docs/plans/rice-knowledge-pilot.md`), with a 30-question test set
(`docs/plans/rice-pilot-test-questions.md`). Two pieces of it were built:

**Baseline runner** (`scripts/rice-pilot/run-eval.ps1` + `questions.json`): asks each test
question in a fresh conversation through the gateway (one clarifying turn allowed), records reply,
time, diagnostic case and tasks into `docs/plans/rice-pilot-results/<run>.md`. Sends/reads UTF-8
bytes explicitly (Windows PowerShell 5.1 would garble Bangla). The baseline run (25 of 30 questions;
photos and real product names still missing) was started against the local stack.

**knowledge-service skeleton** (port 8087, `knowledge_db`; decisions D-A "new service" and
D-B "brute-force search later" as recommended):

| Part | Detail |
|---|---|
| Tables | `knowledge_sources`, `pesticide_products`, `fertilizer_recommendations`, `crop_calendar_entries`, `pest_disease_entries` (all project entity rules) |
| Rule | Every fact row needs an existing `sourceId`; every fact response embeds a short `source` citation (title, publisher, edition, year, page) |
| APIs | CRUD + filtered lists under `/api/v1/knowledge/...`; `GET /pesticides/check?name=&crop=` for the future registered-product guard |
| Import | `POST /api/v1/knowledge/import/{type}` with CSV (no new dependency); rows import independently with a per-row error report; fact rows may name the source by `sourceTitle`; templates in `knowledge-service/import-templates/` |
| Wiring | Gateway route + fallback, compose service, `knowledge_db` in the MySQL init script, CLAUDE.md table |

| Check | Result |
|---|---|
| Unit tests (CSV parser, import mapping/validation/errors) | ✅ 12 passing |
| Docker images for knowledge-service and api-gateway | ✅ built |
| Live smoke test (port 8088, scratch DB `knowledge_smoke_db`, fake "SMOKE TEST" data, dropped afterwards) | ✅ import, case-insensitive duplicates, crop normalisation, source-by-title, citations, product check, Bangla round trip, 404 missing source, 400 for 31 April / bad enum / unknown import type, wrap-around calendar window, country-wide entry matches a region filter, soft delete |
| Bugs found by the smoke test | ① field `regionOrZone` broke startup: Spring Data parsed the `Or` in derived query names as a keyword → renamed `region`. ② unknown `sortBy` on a custom `@Query` returned 500 (`InvalidDataAccessApiUsageException`) → now 400; the catch-all handler now logs what it hides |

No real Bangladeshi data is loaded; that waits for the documents (pilot Phase 0).

## 7c. 7-Day Farm Command Center

The last half-built MVP Core feature: a "this week" view over data that already existed.

| Part | Detail |
|---|---|
| `GET /api/v1/advisor/command-center?farmerId=&days=7` (1–14) | Days from today, each with its planned items and a weather summary (min/max °C, rain, max wind, condition; only within the ~72 h forecast), plus **overdue** items and **recent activity** (farm events, last 7 days, max 10) |
| `WeekPlanBuilder` (pure, unit-tested) | Weather tasks sit on their **recommended** day, others on their due date; past → overdue; beyond the window → left out. FOLLOW_UP tasks are titled "Check on: <symptom>" and carry the case's risk. Daily condition uses the `ForecastFormatter` rule (a rainy hour wins) |
| Chat UI | 📅 button in the header opens a side drawer: overdue (red), 7 day cards (today highlighted, weather line, items with time, title, risk badge, advice, ✓ / Skip), recent activity |

Tests: 8 new `WeekPlanBuilderTest` cases; all 50 ai-advisor unit tests pass (run on a scratch copy so the
running instance wasn't disturbed). They caught one fragility: `casesById.get(null)` throws on immutable
maps; the lookup is now guarded. **Not yet verified live** (ai-advisor can't be restarted during the
baseline run) and not seen in a browser.

## 7d. Found: conversation turns fail when a scheduler touches the farmer's tasks (open)

Baseline question F3 returned **500** after 451 s. Root cause (from the ai-advisor log):
`startConversation` / `continueConversation` are `@Transactional`, and the transaction stays open
for the whole turn **including the multi-minute Ollama call**. MySQL's default REPEATABLE READ gives
the transaction a snapshot from its first read (21:34). At 21:40 the hourly `FollowUpScheduler` and
`WeatherTaskScheduler` updated this farmer's tasks (`@Version` +1). At 21:42 the turn saved its stale
copy of the SPRAY task → `StaleObjectStateException` → 500, and the **whole turn rolled back**: the
farmer's message and the reply were lost.

More likely since Day 11 (`WeatherTaskScheduler` writes hourly) and today's reminder fix
(`FollowUpScheduler` now writes too). Fix: keep the LLM call **outside** the transaction (save the
farmer message → call Ollama → persist reply/case/tasks in a short transaction with fresh reads).
**Not fixed yet.** F3 must be re-run for the baseline (to a separate results file, so `baseline.md`
isn't overwritten).

## 7e. Baseline run: stopped at 10 of 25

The background baseline run was **stopped by Claude Code because the machine ran critically low on
memory** (local stack of 7 Spring services + Ollama 8B + an unrelated visa-rag app with Postgres/Qdrant
containers + builds). Not a script failure.

| Recorded in `docs/plans/rice-pilot-results/baseline.md` | Not run yet |
|---|---|
| F1–F10 (fertiliser group); **F3 is an error** (§7d), so 9 usable answers | P3, P5–P8, V1–V5, T1–T5 (15), plus P1/P2/P4 (photos) and P9/P10 (product names) |

Resume (after the §7d fix, on a quieter machine), into a separate file so `baseline.md` is kept:

```
powershell -ExecutionPolicy Bypass -File scripts\rice-pilot\run-eval.ps1 -Run baseline-part2 -Only F3,P3,P5,P6,P7,P8,V1,V2,V3,V4,V5,T1,T2,T3,T4,T5
```

**Baseline timing caveat:** builds, tests and the smoke instance ran on the same CPU during the
baseline, so its reply times are inflated. Treat the time metric as rough, or re-measure on an idle machine.

---

## 7f. State at End of Session (2026-11-01)

**Committed and pushed today:** `98845b7` (Weather-to-Decision + Best-Time engine), `737794c`
(FollowUpScheduler fix + Day 11 log).

**Everything else from Day 12 is uncommitted** (working tree):

| Area | Files |
|---|---|
| Chat UI: farmer picker, task cards, bell, 📅 week drawer | `api-gateway/src/main/resources/static/index.html` |
| Gateway route for knowledge-service | `api-gateway/.../application.properties`, `FallbackController.java` |
| Docker setup | `docker-compose.yml`, `docker/spring-service/Dockerfile`, `docker/mysql/init/`, `.dockerignore` |
| knowledge-service (new module) | `knowledge-service/` |
| Command Center | `ai-advisor-service/...` `CommandCenterController`, `CommandCenterDto`, `CommandCenterService(Impl)`, `WeekPlanBuilder`, `WeekPlanBuilderTest`, `FarmTaskRepository`, `FarmEventRepository` |
| Rice pilot | `docs/plans/` (plan, test questions, partial baseline results), `scripts/rice-pilot/` |
| Docs | `CLAUDE.md`, `docs/USER_MANUAL.md`, `docs/DAY12-PROGRESS.md` |

Suggested commits (after the checks below): ① UI, ② Docker, ③ knowledge-service + gateway route,
④ Command Center, ⑤ rice pilot docs/scripts + Day 12 log.

**Verification status**

| Item | Unit tests | Live API | Browser |
|---|---|---|---|
| Farmer picker / task cards / bell | n/a | ✅ endpoints they call | 🔲 |
| Command Center | ✅ 8 (50 total in ai-advisor) | 🔲 needs ai-advisor restart | 🔲 |
| knowledge-service | ✅ 12 | ✅ smoke test on scratch DB | n/a |
| Docker | n/a | ✅ config + all 9 images build | 🔲 `docker compose up` not run |

**Left running / left behind on the dev machine**
- Local services started this session with `mvnw` (registry, farmer on **8087**, crop, weather,
  notification, ai-advisor, gateway). They hold ports 8080/8761 needed by Docker. Stop them with:
  `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ? { $_.CommandLine -match 'com\.smartagriculture' } | % { Stop-Process -Id $_.ProcessId -Force }`
- Test data in the local MySQL: farmer "Karim Saheb (pilot)" (`883783ca-…`) with tasks/notifications
  from the baseline; a pending HARVEST task for the Day 11 Dhaka test farmer; one SKIPPED task for
  farmer id `ui-card-test`. The scratch DB `knowledge_smoke_db` was dropped.
- Port 8081 is used by an unrelated app (`VisaRagServiceApplication`); farmer-service can't use it
  locally while that runs (Docker avoids this).

---

## 8. What's Next

| Item | Status |
|---|---|
| Free memory: stop the local `mvnw` services (§7f), switch to `docker compose up -d --build` | 🔲 First thing |
| Browser check: start screen, task cards, bell, 📅 week drawer; Command Center endpoint live | 🔲 |
| Commit the Day 12 work (suggested split in §7f) | 🔲 After the checks |
| Finish the baseline (§7e) | 🔲 After the §7d fix |
| Rice pilot: collect documents (D1–D5), finish baseline (photos, product names), load real data via CSV import | 🔲 Needs the user |
| Rice pilot Phase 2: document chunks + embeddings + search | 🔲 After documents |
| Fix: LLM call inside the conversation transaction (§7d) | 🔲 Recommended next; loses farmer turns |
| 7-Day Farm Command Center | ✅ Built + unit-tested; 🔲 live/browser check |
| Farm asset (plot/pond) picker in the chat | 🔲 Proposed |
| Crop-aware best-time rules, recent rainfall | 🔲 Proposed |

---

*Built with Java Spring Boot · MySQL · Ollama · Open-Meteo · Docker · AI-assisted with Claude Code*
