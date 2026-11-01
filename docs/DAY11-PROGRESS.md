# Smart Agriculture Advisor — Day 11 Progress

> **Project:** Smart Agriculture Advisor
> **Stack:** Java 21 · Spring Boot 4.0.6 · Spring Cloud 2025.1.0 · MySQL 8.x · Ollama · Open-Meteo
> **Date:** 2026-10-29
> **Approach:** AI-assisted development using Claude Code

---

## 1. What Was Built

Two MVP Core features from `docs/prd/agro-agent-core-feature-matrix.md`, built together:

- **Weather-to-Decision Intelligence:** the advisor gets the real forecast **for the farmer's own
  location** instead of "the 5 newest weather rows from anywhere".
- **Best-Time Recommendation Engine:** for weather-sensitive work (spray, fertilise, irrigate,
  harvest, sow) the system picks *when* to do it, explains why, and notifies the farmer hourly as
  the forecast changes.

**Starting point (what was missing):**

| Gap | Where |
|---|---|
| weather-service only had hand-entered records, no forecast source, no location/time filter | `weather-service` |
| Advisor prompt used the latest 5 weather rows globally, regardless of farmer location | `ExternalContextFetcher.fetchWeather()` |
| `FarmTask.weatherDependent` existed but was never `true`; no API to create a task | `ai-advisor-service` |
| `FollowUpScheduler` explicitly skipped weather-dependent tasks ("need a weather-aware trigger") | `FollowUpScheduler` |

**Decisions made before coding** (asked, not assumed):
1. Forecast source: **Open-Meteo** (free, no API key) synced into weather-service.
2. Task creation: **both** a POST API **and** the advisor via a new `TASK:` reply tag.

---

## 2. Architecture

```
 Farmer chat ──▶ ai-advisor-service ──(Feign)──▶ weather-service ──(RestClient)──▶ Open-Meteo
                   │  TASK: SPRAY 1                  GET /weather-records/forecast
                   ▼                                  ├─ cache: FORECAST rows per ~1 km grid point
            FarmTask (weatherDependent)               └─ re-fetch at most every 180 min
                   │
       BestTimeEvaluator (pure rules) ◀── hourly forecast for the farmer's lat/lon
                   │
     WeatherTaskScheduler (hourly) ──▶ notification-service (WEATHER_ALERT)
```

---

## 3. weather-service: Open-Meteo Forecast

| Piece | What it does |
|---|---|
| `client/OpenMeteoClient` | `GET /v1/forecast` with hourly temp/humidity/precipitation/wind/weather_code, 4 days, **UTC**. Maps WMO codes → existing `WeatherCondition` enum. Converts UTC → JVM zone so times compare with `LocalDateTime.now()` everywhere else (DAY10 §6). |
| `WeatherService.getHourlyForecast` | Rounds lat/lon to 2 dp (~1 km, nearby farmers share a cache). Syncs if the last sync is older than `weather.forecast.refresh-minutes` (180), **upserting** rows by (lat, lon, hour, FORECAST). If Open-Meteo is down, it serves whatever is cached and logs a warning. |
| `GET /api/v1/weather-records/forecast` | `countryCode`, `region`, `latitude`, `longitude`, `hours` (1–72). Validated. No gateway change needed. |
| `GlobalExceptionHandler` | New `HandlerMethodValidationException` → 400 with `fieldErrors`. |

**Deviations from the plan, and why:**
- No separate `ForecastService`: the method lives on `WeatherService` to reuse the existing private
  `toResponse` mapper.
- **Freshness is tracked in memory**, not via `updatedAt`. Hibernate skips the UPDATE (and the
  `@UpdateTimestamp`) when a re-fetched hour hasn't changed, so `updatedAt` would make the cache
  look permanently stale. Losing the map on restart only costs one extra fetch.
- `HandlerMethodValidationException` rather than `ConstraintViolationException`: Spring 6.1+
  validates `@RequestParam` constraints natively and throws the former. **Side fix:** the existing
  `@Max(100)` on the list endpoint's `size` previously fell through to the catch-all → **500**; it
  now returns 400.
- `RestClient.builder()` instead of an injected `RestClient.Builder`: Spring Boot 4 only
  auto-configures the builder via the separate `spring-boot-starter-restclient`. This was **caught
  at the first live start-up** (`APPLICATION FAILED TO START`); the unit tests don't load a Spring
  context, so they couldn't catch it.

---

## 4. ai-advisor-service: Best-Time Engine

### 4.1 `BestTimeEvaluator`: rules, not LLM

Deliberately deterministic: the same forecast always gives the same answer, with an exact reason.
It searches whole hours from **24h before to 48h after** the planned time (never before the next
full hour, never past the forecast) and picks the **earliest** slot that passes:

| Task | Allowed hours | Must hold |
|---|---|---|
| SPRAY | 06–10, 16–18 | wind ≤ 15 km/h, temp ≤ 32 °C, rain < 1 mm across slot + next 6 h |
| FERTILIZE | 06–17 | rain ≤ 1 mm in slot, < 10 mm over next 24 h (washout) |
| IRRIGATE | 05–09, 16–19 | rain < 5 mm over next 24 h (rain does the job) |
| HARVEST | 07–16 | rain < 0.5 mm across slot + next 6 h, wind ≤ 30 km/h |
| SOW | 06–17 | rain < 20 mm over next 24 h (seed washout) |

Results: `RECOMMENDED` (with time + reason the earlier slots were skipped), `NO_WINDOW` ("No safe
time to spray before …: <reason>", or "No need to irrigate…"), or `NO_FORECAST` (no coordinates,
forecast down, or the forecast doesn't reach the planned date yet).

### 4.2 Task API

| Endpoint | Behaviour |
|---|---|
| `POST /api/v1/advisor/farm-tasks` | `farmerId`, `taskType` (`SPRAY`/`FERTILIZE`/`IRRIGATE`/`HARVEST`/`SOW`), `dueDate`, optional `farmAssetId`, `priority`. Evaluated immediately → 201 with `recommendedStartAt` + `weatherAdvice`. Same pending farmer/asset/type → **409**. Unknown type → 400. |
| `GET /api/v1/advisor/farm-tasks/{id}/best-time` | Re-evaluates against the latest forecast. Non-weather task → 400. |

`FarmTask` gained `recommendedStartAt`, `weatherAdvice`, `weatherCheckedAt` (exposed) and
`lastNotifiedSlot`, `startNowNotified` (internal).

### 4.3 `WeatherTaskScheduler` (hourly)

For pending weather tasks due within 72h (one forecast fetch per farmer per run), it notifies
**only on change**:
- the recommended **3-hour block** changes → `WEATHER_ALERT` "Weather plan: spray on Fri 30 Oct 06:00";
- the slot starts within the hour → "Good time to spray from …", once per slot;
- no safe slot → "hold off" once; no forecast and the task is due → plain reminder once.

Failed sends aren't recorded, so the next run retries. Titles carry a date because
notification-service rejects an identical PENDING title for the same farmer (409).

### 4.4 Advisor integration

- **Prompt:** the forecast for the farmer's lat/lon, as ~16 three-hour block lines
  (`ForecastFormatter`), replaces the global "recent weather" list when available.
- **New `TASK:` tag** (`TASK: SPRAY 1` / `NONE`), parsed by `AdvisoryResponseParser` (last line
  wins, so a later `NONE` cancels; days capped at 14; line-anchored, so "TASK:" in prose is
  ignored) and stripped from the visible text.
- The suggested task is **upserted** (the model repeats itself across turns → the due date moves,
  no 409), linked to the diagnostic case, and evaluated.
- **Bug prevented:** `upsertFollowUpTask` looked a task up by `sourceDiagnosticCaseId` expecting
  one result. A case can now source a FOLLOW_UP *and* a weather task, which would have crashed the
  next turn with a non-unique result. The lookup is now scoped to `taskType = FOLLOW_UP`.

### 4.5 Also in this change

- `GlobalExceptionHandler` (ai-advisor): `IllegalArgumentException` → 400, and
  `HttpMessageNotReadableException` (bad JSON / unknown enum) → 400. Both were 500s before.

### 4.6 Fixed: `FollowUpScheduler` reminder 409s and miscount (2026-11-01)

The pre-existing issue found during live testing (§8) was fixed in a follow-up session.

**Root cause:** plain (non-weather) task reminders were sent every hour and never recorded. From the
second run on, notification-service rejected the identical `"Reminder: X"` title with 409. The
failure was only logged as a warning, and the summary line counted *due tasks*, not successful sends.

| Change | Detail |
|---|---|
| Remind once per due date | New `FarmTask.reminderSentFor` stores the due date the reminder was sent for; already-reminded tasks are skipped. A moved due date (e.g. the advisor resets a follow-up) re-arms it. Failed sends aren't recorded → retried next run. |
| Dated titles | `Reminder: FOLLOW_UP (due Thu 29 Oct 08:00)`, same approach as `WeatherTaskScheduler` (§4.3). |
| Honest log line | Counts notifications actually sent. |
| **Side fix:** soft-deleted tasks | The reminder query lacked `DeletedFalse`, so soft-deleted tasks still got reminders. Renamed to `findByStatusAndWeatherDependentFalseAndDeletedFalseAndDueDateLessThanEqual`. |
| **Side fix:** case follow-up title | Every case used the same title `"Follow-up: how is your crop doing?"`. A farmer's second case would 409 hourly and stay stuck in `AWAITING_FOLLOWUP`. The title now carries the follow-up due date. |

The new column is added by `ddl-auto=update`. Existing due tasks have it empty, so each gets one
final reminder (with the new title) on the first run after deployment.

---

## 5. Tests

| Test | Cases | Covers |
|---|---|---|
| `BestTimeEvaluatorTest` | 14 | every task type's rules, rain/wind/heat rejection with reason text, earliest-slot choice, NO_WINDOW, NO_FORECAST, expired date, date beyond forecast |
| `WeatherTaskSchedulerTest` | 8 | notify-once, same-block shift is silent / new block re-notifies, "good time now" once, NO_WINDOW once, NO_FORECAST only when due, failed send retried |
| `AdvisoryResponseParserTest` | 14 (+6) | `TASK:` parse/strip, default 0 days, cap, NONE/unknown, last-wins, prose not treated as a tag |
| `FollowUpSchedulerTest` | 4 | reminder once per due date, rescheduled task re-reminded, failed send not recorded, dated case title + failure leaves case awaiting (§4.6) |
| `ForecastFormatterTest` | 2 | 3-hour grouping, rain sum, max wind/temp, rainy condition preferred |
| `OpenMeteoClientTest` (weather) | 4 | array mapping, UTC→Asia/Dhaka, missing/short arrays, WMO codes |
| `WeatherServiceImplTest` (weather) | 1 | coordinate rounding |

**47 unit tests, all passing** (43 + 4 from §4.6). None needs MySQL, Ollama or the network.

---

## 6. Live Verification

Stack: MySQL, Eureka, farmer, weather, notification, ai-advisor and gateway, plus Ollama
(`qwen3:8b`). Test farmer: Dhaka (23.8103, 90.4125).

| # | Check | Result |
|---|---|---|
| 1 | Forecast via gateway | ✅ 48 hourly rows from Open-Meteo. The temperature peak (~31 °C) falls at **14:00 local**, which confirms the UTC→+06:00 conversion. |
| 2 | Cache | ✅ 2nd call 29 ms vs 2.2 s, byte-identical, exactly one "Synced 96 … hours" log line |
| 3 | Validation | ✅ `latitude=95&hours=100` → 400 with both field errors |
| 4 | POST SPRAY task (due Fri 08:00, created Thu 21:30) | ✅ 201, `recommendedStartAt` Fri 06:00. Hand-checked: first allowed spray hour after now, dry, wind 5–8 km/h. |
| 5 | Duplicate / bad type / missing fields | ✅ 409 / 400 / 400 with `fieldErrors` |
| 6 | Scheduler on restart | ✅ one `WEATHER_ALERT` "Weather plan: spray on Fri 30 Oct 06:00", priority HIGH |
| 7 | Scheduler on 2nd restart | ✅ query + task update seen in SQL log, **no** second notification |
| 8 | Chat: "Should I spray tomorrow?" | ✅ `TASK:` hidden from the farmer, existing SPRAY task **updated** (not 409) and linked to the case; a separate FOLLOW_UP task created alongside (the §4.4 lookup fix in action) |
| 9 | Chat: urea top-dressing | ✅ new FERTILIZE task **created**; reply ends with "Forecast check: Best time to apply fertiliser: Fri 30 Oct 06:00." |

---

## 7. Found During Live Testing: the LLM's own timing is unreliable

Chat 1's reply said *"Spray tomorrow morning (00-03 or 03-06) when it's dry"*. The engine had
correctly chosen **06:00**, but the farmer would have read "midnight" in the chat and then received
a notification saying 06:00.

What was tried, in order:
1. **Append the engine's verdict to the reply** ("Forecast check: …"), so the chat always agrees
   with the notifications. ✅ Kept: this is the real fix.
2. **Prompt hint:** "Field work happens in daylight: 6-10am or 4-6pm, never night hours." Kept, but
   on its own it wasn't enough: chat 2 still said "03-06 (early morning)" and "09-12 (late
   afternoon)".
3. **Clearer forecast lines:** `06:00-09:00 (early morning)` instead of `06-09`. This helped. In
   chat 3 the model read the times and the midday rain correctly, but still chose **03:00–06:00**,
   the latest block before the rain.

**Conclusion:** `qwen3:8b` reads the forecast but doesn't reliably apply daylight-hours reasoning.
The rule-based "Forecast check" line is the source of truth, and the notifications use the same
rule. In chat 2 the model also wrote `TASK: FERTILIZE 1` where the farmer said "the day after
tomorrow"; the ±24h/48h search window absorbs that.

---

## 8. Known Issues

| Issue | Status |
|---|---|
| LLM prose can still suggest a poor time (above). The "Forecast check" line is correct. | 🔲 Model limitation; a larger model or structured output could fix it |
| **Pre-existing:** `FollowUpScheduler` re-sends plain reminders hourly; notification-service rejects them with 409 because the title is identical, yet the log says "sent 2 task reminders" (it counts tasks, not successful sends) | ✅ Fixed 2026-11-01 (§4.6), unit-tested; not yet re-verified live |
| Forecast rows accumulate (one per hour per grid point); no pruning yet | 🔲 Fine at this scale |
| Weather tasks are API/chat only; no task cards in the Chat UI | 🔲 Out of scope (PRD: 7-Day Farm Command Center) |
| Rules are per task type, not per crop; past rainfall ("field still wet") isn't used | 🔲 Out of scope |

---

## 9. What's Next

| Item | Status |
|---|---|
| Weather-to-Decision + Best-Time Recommendation | ✅ Built, unit-tested, verified live |
| Fix `FollowUpScheduler` reminder 409s / miscount | ✅ Done (§4.6) |
| Show weather tasks + "Forecast check" as cards in the Chat UI | 🔲 Proposed |
| Dynamic Farming Timeline (MVP Core) | 🔲 Can build on `FarmEvent` + the new tasks |

---

*Built with Java Spring Boot · MySQL · Ollama · Open-Meteo · AI-assisted with Claude Code*
