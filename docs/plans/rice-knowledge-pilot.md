# Pilot Plan — Bangladesh Rice Knowledge Layer

> **Status (2026-11-01):** Phase 0 started (test set + baseline runner; baseline **10 of 25 questions recorded**, run stopped by low memory, see `DAY12-PROGRESS.md` §7e). Phase 1 **skeleton built**: `knowledge-service` with tables, CRUD, lookups and CSV import, no real data yet. Phases 2–5 not started.
> **Goal:** ground the advisor's rice advice in official Bangladeshi sources, prove the method
> on one crop, and measure the improvement before extending it to other crops.

---

## 1. Why a pilot

Today the advisor's farming knowledge (diagnosis, pesticides, fertiliser doses, varieties,
planting dates) comes from the LLM's general training (`qwen3:8b`, `llava:7b`). It is not
Bangladesh-specific, shows no sources, and can invent a product name or dose. Only the weather
forecast (Open-Meteo) is real data.

The pilot does the "real data" work for **rice only**, end to end, so that:

1. rice advice is correct, legal in Bangladesh and cites its source;
2. the method (collect → structure → retrieve → cite → guard) is proven on one crop before scaling;
3. the reusable parts (tables, import, retrieval, citations, safety guard) are built once;
4. the improvement is **measured** with a before/after test, not just felt.

**Why rice:** the main crop of Bangladesh (Aus, Aman and Boro seasons); comparatively well-organised
sources (BRRI); covers every data type (varieties, calendar, fertiliser, pests and diseases,
weather timing); and the PRD persona (Karim Saheb, 3 rice plots) gives ready-made test scenarios.

---

## 2. Scope

**In scope**
- Rice: varieties, crop calendar per season, fertiliser recommendations, registered pesticides,
  major pests and diseases.
- English sources first; Bangla sources if Phase 0 shows they're needed and usable (see §9).
- The advisor using this data in chat and photo diagnosis, with visible sources.

**Out of scope (later)**
- Other crops, aquaculture, market prices, financial advice.
- Live pest-outbreak feeds, real-time advisories.
- Real authentication or multi-tenant concerns.

---

## 3. Data to collect (Phase 0 checklist)

These are the organisations known to publish this material. **Current URLs, editions, formats and
licence terms have not been verified**; confirming them is part of Phase 0. Record for every
document: title, publisher, edition/year, URL, date retrieved, language, format, licence/terms.

| # | Data | Likely source | What we need from it | Used for |
|---|---|---|---|---|
| D1 | **Registered pesticide list** | DAE, Plant Protection Wing | Trade name, active ingredient, formulation, registration no., target pest/disease, crop, dose and unit, pre-harvest interval, toxicity class, registration date/status | Only recommend legal products, at the official dose |
| D2 | **Fertiliser recommendations** (rice section) | BARC *Fertilizer Recommendation Guide* (latest edition); SRDI upazila soil guides | Nutrient and product doses (urea, TSP, MoP, gypsum, zinc) per season, region/zone and soil fertility; split-application schedule | "How much urea, and when?" |
| D3 | **Rice varieties** | BRRI variety descriptions (BRRI dhan series), Rice Knowledge Bank | Name, season, duration, yield, special traits (salt/flood/drought tolerance), suitable regions | "Which variety should I plant?" |
| D4 | **Crop calendar** | DAE / BRRI | Seedbed, transplanting, fertiliser and harvest windows per season (Aus/Aman/Boro) and region | "When should I plant?", default task dates |
| D5 | **Pest and disease guides** | BRRI disease and insect-pest guides; DAE *Krishoker Janala* | Name (EN + BN), symptoms, favourable conditions, cultural and chemical management (by active ingredient) | Grounds chat and photo diagnosis |
| D6 | **Agro-met guidance** (optional) | BMD / BAMIS agro-met bulletins | Spraying/fertilising weather guidance | Check `BestTimeEvaluator` thresholds |
| D7 | **Local units** | Agronomist / DAE | Decimal, bigha (varies by region), katha ↔ hectare | Doses farmers can apply |

**Who does what:** you collect the documents (D1–D5 minimum) into a local folder and confirm the
terms allow this use. Once they're on disk, I read and assess them (format, extractability,
language) and turn the tables into import files.

---

## 4. Architecture

```
 Farmer chat ──▶ ai-advisor-service ──(Feign)──▶ knowledge-service (new, :8087, knowledge_db)
                    │                                ├─ structured: pesticides, fertiliser,
                    │                                │   calendar, pests/diseases, sources
                    │                                └─ documents: chunks + embeddings
                    │                                         │
                    │                                         └─(embed)──▶ Ollama nomic-embed-text
                    ├─ prompt gets a numbered REFERENCE block  [S1] [S2] ...
                    ├─ RegisteredProductGuard checks the reply
                    └─ reply + "Sources" ──▶ Chat UI
 crop-service: rice varieties stored as Crop rows (name=Rice, variety=BRRI dhan…) — no schema change
```

### 4.1 New service: `knowledge-service`
Follows every rule in `CLAUDE.md` (UUID, soft delete, `@Version`, audit, Request/Response DTOs,
`/api/v1/knowledge/...`, paging, `GlobalExceptionHandler`, Eureka). Port **8087**, DB `knowledge_db`;
added to `docker-compose.yml` and `docker/mysql/init/01-create-databases.sql`.

**Why a new service rather than extending crop-service:** pesticides, fertiliser rules and
documents are a separate domain from the crop catalogue, they change on different schedules
(e.g. pesticide registrations), and retrieval adds embedding logic crop-service shouldn't carry.
*(Decision D-A in §10.)*

### 4.2 Data model (knowledge_db)

| Table | Key fields |
|---|---|
| `sources` | title, publisher, edition, year, url, language, licence_note, retrieved_at |
| `pesticide_products` | trade_name, active_ingredient, formulation, registration_no, crop, target (pest/disease), dose_value, dose_unit, dose_basis (per litre / per ha / per decimal), pre_harvest_interval_days, toxicity_class, registration_status, source_id |
| `fertilizer_recommendations` | crop, season (AUS/AMAN/BORO), region (region, district or agro-ecological zone), soil_fertility_class, urea/tsp/mop/gypsum/zinc kg_per_ha, split_schedule, source_id |
| `crop_calendar_entries` | crop, season, region, activity (SEEDBED/TRANSPLANT/FERTILIZE/HARVEST…), window_start, window_end, source_id |
| `pest_disease_entries` | name_en, name_bn, crop, kind (PEST/DISEASE), symptoms, favourable_conditions, cultural_control, chemical_control_active_ingredients, source_id |
| `knowledge_chunks` | source_id, page, language, text, embedding (stored as binary float array) |

Rice **varieties** go into crop-service's existing `Crop` table (`variety`, `season`,
`growingDurationDays`, `typicalYieldPerHectare`, `region` already exist).

### 4.3 Retrieval
- **Chunking:** ~300–500 words per chunk with overlap, keep page numbers.
- **Embeddings:** Ollama `nomic-embed-text` (already pulled). It is English-centric; if Bangla
  sources are in scope and retrieval quality is poor, test a multilingual embedding model
  available in Ollama (e.g. `bge-m3`, availability to be confirmed). *(Decision D-C.)*
- **Search:** brute-force cosine similarity in Java over the rice chunks (a few thousand at most)
  for the pilot. No new infrastructure. Move to a vector store only if scale demands it.
  *(Decision D-B.)*
- **API:** `POST /api/v1/knowledge/search` `{query, crop, region, topK}` → passages with source
  and page; plus structured lookups, e.g. `GET /api/v1/knowledge/fertilizer?crop=RICE&season=BORO&region=…`,
  `GET /api/v1/knowledge/pesticides?crop=RICE&target=…`.

### 4.4 Advisor integration (ai-advisor-service)
- `ExternalContextFetcher.fetchKnowledge(question, farmer)` — called next to the existing
  `fetchFarmer` / `fetchCrops` / `fetchForecast`; empty list on failure (same graceful-degradation
  pattern, never blocks the reply).
- `PromptTemplates`: a numbered **REFERENCE MATERIAL** block (top 3 passages + matching structured
  rows, capped at ~1,500 tokens to keep CPU latency acceptable) and rules:
  only recommend pesticides listed in the reference material; give doses only from it; cite as
  `[S1]`; if the material doesn't cover it, say so and suggest the local agriculture officer (SAAO).
- **`RegisteredProductGuard`** (rules, not LLM — same philosophy as `BestTimeEvaluator`): after the
  reply, match product/active-ingredient names against `pesticide_products`; an unregistered name
  gets a visible warning appended and is logged for the evaluation.
- `ConversationDto.Response` gains `sources` (title, publisher, page); the Chat UI shows them
  under the reply.

---

## 5. Phases

| Phase | Work | Owner | Exit criteria |
|---|---|---|---|
| **0. Collect & baseline** | Collect D1–D5 + terms; write the 30-question test set (§6); **record today's answers as the baseline** | You (docs, agronomist contact); me (test set draft, baseline run) | Checklist complete; baseline answers saved |
| **1. Structured data** | knowledge-service skeleton; tables; CSV import templates; extract tables from the documents; load rice varieties into crop-service | Me (you check a sample of extracted rows against the documents) | All D1–D4 tables loaded; 20 random rows hand-checked, 0 errors |
| **2. Documents & retrieval** | Chunk + embed D5 (and other prose); search API | Me | Top-3 retrieval contains the right passage for ≥ 80% of test questions |
| **3. Advisor integration** | Reference block, citations, `RegisteredProductGuard`, sources in API + UI; unit tests | Me | Unit tests pass; live chat shows cited answers |
| **4. Evaluate** | Re-run the 30 questions; score before vs after; agronomist review of a sample | Me (run + score); agronomist (review) | Targets in §6 met or gaps understood |
| **5. Decide** | Go / adjust / stop; plan the next crop | You | Written decision |

Effort is driven mostly by Phase 0 (how long the documents take to obtain) and Phase 1 (how
extractable their tables are). Scanned PDFs or Bangla-only tables can multiply Phase 1.

---

## 6. Measuring success

**Test set:** 30 rice questions, drawn from the Karim Saheb scenarios and real farmer phrasing
(full set, scoring sheet and answer-key template: `rice-pilot-test-questions.md`):

| Group | Count | Example |
|---|---|---|
| Fertiliser | 10 | "How much urea for 1 bigha of Boro rice, and when?" |
| Pest / disease | 10 | "Brown spots on my Aman rice leaves — what should I spray?" (some with photos) |
| Variety / calendar | 5 | "Which variety for a flood-prone field this Aman season?" |
| Timing / weather | 5 | "Can I spray tomorrow?" |

Include a few in Bangla if Bangla is in scope.

| Metric | Baseline (today) | Target |
|---|---|---|
| Recommends only DAE-registered pesticides | measured in Phase 0 | 100% |
| Dose matches the official source | measured | 100% of answers that give a dose |
| Answer cites a source | 0% | 100% of factual recommendations |
| Says "not covered, ask your SAAO" instead of guessing when sources are silent | measured | ≥ 90% |
| Agronomist rates "safe and useful" | measured (sample) | clear majority, no unsafe answers |
| Reply time (CPU) | measured | no more than ~25% slower |

---

## 7. Safety

- Pesticide advice only from `pesticide_products`, with dose, pre-harvest interval and a
  protective-equipment reminder.
- Every recommendation shows its source and edition year, so an outdated list is visible.
- Unregistered names are flagged by the guard, never silently passed.
- Low confidence / high risk keeps using the existing escalation path (Day 7 Confidence/Risk tags).
- A standing disclaimer: advice supports, not replaces, the local agriculture officer.

---

## 8. Reuse for the next crop

| Built once | Next crop needs |
|---|---|
| knowledge-service, tables, APIs | New rows |
| Import templates and extraction scripts | New source files |
| Chunking, embedding, search | New documents |
| Reference block, citations, guard, UI sources | Nothing |
| 30-question evaluation method | A new question set |

---

## 9. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Licence/terms don't allow reuse | Can't use a source | Check in Phase 0; prefer clearly public government publications; store only what's needed |
| Scanned or Bangla-only PDFs | Slow extraction | Assess in Phase 0; OCR; hand-transcribe small tables; you spot-check |
| `nomic-embed-text` weak on Bangla | Poor retrieval | Measure in Phase 2; try a multilingual model (D-C) |
| `qwen3:8b` ignores the reference material | Wrong answers persist | Prompt rules + `RegisteredProductGuard` + measured in Phase 4; consider a larger model if needed |
| Pesticide list goes out of date | Recommends deregistered products | Store registration status and retrieval date; show the edition; re-import periodically |
| Local units (bigha varies by region) | Wrong quantities | Unit table confirmed by an agronomist; prefer decimal/hectare in answers |
| Longer prompts slow CPU inference | Worse UX | Cap the reference block; measure reply time |
| No agronomist available | Can't validate safety | Treat results as provisional; don't present as field-ready |

---

## 10. Decisions needed before Phase 1

| ID | Decision | Recommendation |
|---|---|---|
| D-A | New `knowledge-service` vs extend crop-service | New service (§4.1) |
| D-B | Vector search: brute force in Java vs a vector database | Brute force for the pilot |
| D-C | Bangla sources in scope for the pilot? | Decide after Phase 0 shows what exists |
| D-D | Agronomist / extension officer for review | Identify one in Phase 0 |

---

## 11. Next actions

1. **You:** start collecting D1–D5 (save them in one local folder, note the terms for each).
2. **Me:** draft the 30-question test set and run the baseline against the current system.
3. **Together:** decide D-A to D-D, then start Phase 1.
