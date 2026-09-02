# Smart Agriculture Advisor — Day 9 Progress

> **Project:** Smart Agriculture Advisor
> **Stack:** Java 21 · Spring Boot 4.0.6 · Spring Cloud 2025.1.0 · MySQL 8.x · Ollama
> **Date:** 2026-09-02
> **Approach:** AI-assisted development using Claude Code

---

## 1. What Was Attempted

Follow-up to `DAY8-PROGRESS.md` §4: the image-diagnosis feature's application code was complete
and verified, but unusable because `gemma4:latest`'s vision path in this Ollama build silently
dropped the image once the accompanying prompt exceeded ~15–20 tokens (~12 words) — too tight for
any real farmer message. The user pulled `llava:7b` (a mature, widely-used Ollama vision model) as
a replacement.

---

## 2. What Was Changed

| Change | File |
|---|---|
| `ollama.api.vision-model` switched from `gemma4:latest` to `llava:7b` | `ai-advisor-service/src/main/resources/application.properties:34` |

No code changes were needed — `OllamaApiClient.getAdvice(...)` already routes to
`ollama.api.vision-model` whenever an image is attached (see Day 8), so the config swap alone was
sufficient.

---

## 3. Verification

**Step 1 — direct-to-Ollama sanity check** (same methodology as the Day 8 bisection), a realistic
~50-word farmer message + the same test photo used in Day 8:

- `prompt_eval_count: 632` (vs. gemma4's failure point of ~92) — the image was clearly ingested.
- Model gave a real, image-grounded description ("yellow spots and brown edges... bacterial leaf
  spot") followed by a structured treatment plan. No "I cannot see the photo" failure.

**Step 2 — full end-to-end through the running stack.** `service-registry` and
`ai-advisor-service` were not running at session start (only `api-gateway`, started separately via
IntelliJ, was up); both were started via `mvn spring-boot:run` and confirmed registered in Eureka.
A real `POST /api/v1/advisor/conversations` request through `api-gateway` (port 8080), with the
same farmer message + photo:

```json
{
  "farmerId": "test-farmer-vision-e2e",
  "queryType": "PEST_CONTROL",
  "message": "My tomato plant leaves have these yellow spots...",
  "imageBase64": "<...>"
}
```

produced a real AGENT reply engaging with the actual photo ("The photo shows some yellowing and
browning on the leaves of the tomato plants...") and a `latestCase` diagnostic record — confirming
the fix holds through the full gateway → conversation → Ollama path, not just direct-to-Ollama.

**Conclusion: the Day 8 vision-dropping bug is resolved by using `llava:7b` instead of
`gemma4:latest`. Image-based crop diagnosis is now functionally usable end-to-end.**

---

## 4. Found and Fixed: `AdvisoryResponseParser` couldn't handle `llava:7b`'s tag-block formatting

`CONVERSATION_SYSTEM_PROMPT` (`PromptTemplates.java:34`) instructs the model to end every reply
with a line containing only `---` followed by the `CONFIDENCE`/`RISK`/`FOLLOW_UP_DAYS`/`ESCALATE`
tag block. The original `AdvisoryResponseParser` located that block with a single regex anchored on
the *first* `---` it found in the reply, on the assumption the block only ever appears once, at the
very end.

**Investigation (each finding pulled from a real raw model reply via a temporary debug log, added
and removed cleanly — not speculation):**

| # | `llava:7b` reply shape observed | Effect on the old parser |
|---|---|---|
| 1 | Tag block at the end, but with **no `---` separator** at all (just a blank line) | No tag block found → silently fell back to wrong defaults (`CONFIDENCE: MEDIUM`, `RISK: LOW` returned instead of the model's actual `LOW`/`MEDIUM`) **and** left the raw `CONFIDENCE:`/`RISK:`/... lines un-stripped in the farmer-visible text |
| 2 | Tag block emitted **twice** — once right after an opening `---`, once again at the true end | The old regex matched from the *first* `---` to end-of-string, swallowing the entire diagnosis (all prose between the two blocks) into the "tag block" — farmer-visible `content` came back **empty** |
| 3 | Tag block placed **before** the diagnosis prose (`--- \n CONFIDENCE:... \n\n <diagnosis text>`), with no second `---` to close it | Same as #2: everything after the single `---` — tags *and* the real diagnosis — was treated as the tag block, so `content` was empty again |

**Fix:** rewrote `AdvisoryResponseParser` (`AdvisoryResponseParser.java`) to stop treating the tags
as one contiguous block. It now matches and strips each `CONFIDENCE:`/`RISK:`/`FOLLOW_UP_DAYS:`/
`ESCALATE:` line individually (plus any standalone `---` divider lines), taking the *last* value
seen for each tag if the model repeats them, and keeps everything else — regardless of where it
falls relative to the tags — as the farmer-visible text.

**Verification:** after the rewrite, ran the same end-to-end request (`api-gateway` →
`ai-advisor-service` → `llava:7b`) 3 more times to sample the model's format variance. All 3 came
back with full, correctly-formatted diagnosis/clarifying-question text and accurate
`confidenceLevel`/`riskLevel` in `latestCase` — no empty content, no leaked tag lines, in any of the
three observed reply shapes above.

---

## 5. What's Next

| Item | Status |
|---|---|
| Image-diagnosis working end-to-end (vision ingestion) | ✅ Fixed — `llava:7b` resolves the Day 8 blocker |
| `AdvisoryResponseParser` tag-block parsing for `llava:7b`'s output format | ✅ Fixed — rewritten to strip tags line-by-line instead of assuming one contiguous trailing block |
| Voice-First Assistant (Bangla ASR/TTS) | 🔲 Still undecided — separate from this session's work |
| Day 8's known issues (`createdAt`/`updatedAt: null` on `POST`, timezone display quirk) | 🔲 Still carried over, unaddressed |

---

*Built with Java Spring Boot · MySQL · Ollama · AI-assisted with Claude Code*
