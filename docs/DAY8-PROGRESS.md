# Smart Agriculture Advisor — Day 8 Progress

> **Project:** Smart Agriculture Advisor
> **Stack:** Java 21 · Spring Boot 4.0.6 · Spring Cloud 2025.1.0 · MySQL 8.x · Ollama (gemma4:latest for vision)
> **Date:** 2026-09-01
> **Approach:** AI-assisted development using Claude Code

---

## 1. What Was Attempted

Phase 1b of the roadmap (`CLAUDE.md`) deferred two MVP Core features because they needed a
provider decision that hadn't been made: **Voice-First Assistant** (Bangla ASR/TTS) and
**Image-based Crop Diagnosis** (computer vision). This session unblocked the vision half: `ollama
show gemma4:latest` confirmed the locally-pulled `gemma4:latest` model (8B, same class as the
`qwen3:8b` already used for text) has `vision` in its capability list — no new provider, API key,
or service required.

**Outcome: the application code is complete, correct, and verified — but the feature is not
usable end-to-end today because of a limitation in the local Ollama build's handling of
`gemma4`'s vision path (see §4). The code ships as a forward-compatible foundation; it will start
working once that runtime issue is resolved upstream.**

---

## 2. What Was Built

Extends the multi-turn conversation pipeline added in Phase 1 (`ConversationalAdvisorServiceImpl`)
— the original one-shot `/api/v1/advisor/advice` endpoint (`AiAdvisorServiceImpl`) was left
untouched, same as every prior session's changes.

| Change | File(s) |
|---|---|
| New `ollama.api.vision-model=gemma4:latest` config, alongside the existing `ollama.api.model=qwen3:8b` | `ai-advisor-service/src/main/resources/application.properties` |
| `OllamaApiClient.getAdvice(...)` overload accepts a `List<String> images`; routes to `gemma4:latest` only when images are present, otherwise unchanged `qwen3:8b` behavior | `client/OllamaApiClient.java` |
| `Message` entity gains a nullable `imageBase64` (`LONGTEXT`) column; `content` relaxed to nullable for photo-only turns | `entity/Message.java` |
| `StartRequest`/`MessageRequest` accept an optional base64 `imageBase64` field; `MessageView` exposes a `hasImage` flag without echoing the blob back | `dto/ConversationDto.java` |
| New `IMAGE_ANALYSIS_INSTRUCTION` prompt block, appended to the user message only when an image is attached | `service/PromptTemplates.java` |
| `processTurn`/`buildUserMessage`/`upsertDiagnosticCase` thread the image through storage, prompt building (`[with photo]` case-symptom prefix), and the Ollama call | `service/ConversationalAdvisorServiceImpl.java` |
| Attach-photo button, client-side downscale (≤1024px)/compress (JPEG q0.7) via canvas, thumbnail preview, and inline photo bubbles | `api-gateway/.../static/index.html` |
| Fixed an unrelated pre-existing compile error (truncated string literal) blocking `api-gateway` from starting at all | `api-gateway/.../controller/FallbackController.java` |

All of the above was verified directly: `mvn compile` clean, MySQL schema updated cleanly
(`ddl-auto=update` added the new column with no errors), a real end-to-end request through the
gateway correctly stored the image (`hasImage: true` in the response, base64 not echoed back), and
Ollama's own server log confirmed `gemma4:latest` — not `qwen3:8b` — was loaded for image-bearing
turns and left untouched for text-only ones.

---

## 3. Known Issues — Not Fixed (Out of Scope)

Carried over from Day 7, still applicable (`createdAt`/`updatedAt: null` on `POST` create
responses; JVM/MySQL vs. Git Bash timezone display quirk) — see `DAY7-PROGRESS.md` §10.

---

## 4. New Known Issue: gemma4 vision only engages on very short prompts (this Ollama build)

While the request/response plumbing above is confirmed correct, live verification with a real
test photo through the deployed app produced a response where the model claimed it couldn't see
the photo, even though the image was present and correctly base64-encoded in the request.

**Investigation (each step a real request against the running stack, not speculation):**

| # | Test | Result |
|---|---|---|
| 1 | App: real conversation start, photo + farmer message (~20 words), through the gateway | ❌ Model: *"I cannot see the photo you mentioned"* |
| 2 | Direct to Ollama, bypassing the app: short ~12-word message + same photo, no system prompt | ✅ Model engaged with real image content (described colors/shapes, though inaccurately) |
| 3 | App re-run with a debug log confirming `hasImages=true`, `imageCount=1` reached `OllamaApiClient` | Payload confirmed correct — ruled out an app-side serialization bug |
| 4 | Direct to Ollama: app's system prompt + short user text + image (separate system/user messages) | ❌ Failed — `prompt_eval_count` matched text-only token count (no vision tokens) |
| 5 | Direct to Ollama: same content merged into a single user-role message (no system message) | ❌ Still failed — ruled out system/user role structure as the cause |
| 6 | Direct to Ollama: explicit `num_ctx=8192` (vs. the server's default 4096) | ❌ Still failed — ruled out context-window size |
| 7 | Direct to Ollama: bisection on prompt length — 46 words (119 tokens) and 23 words (92 tokens) | ❌ Both failed |

**Finding:** the only request that worked used ~12 words of accompanying text (`prompt_eval_count:
80`); a 23-word message (`prompt_eval_count: 92`) already fails. That is a margin of roughly 10–15
tokens — too tight to fit any real farmer message, let alone the instructional framing the feature
needs. In every failing case, the model's own reasoning trace explicitly states it received no
image data, confirming this is the vision pipeline silently dropping the image rather than the
model choosing to ignore it.

**Conclusion:** this is a genuine limitation/bug in the local Ollama build's handling of
`gemma4:latest`'s vision path once the prompt grows past a very small size — not something fixable
by changing how `ai-advisor-service` constructs its request (routing, payload shape, and role
structure were all ruled out). `gemma4` is a very recently available model; this is consistent with
rough edges in an early Ollama integration rather than a fundamental model limitation.

**Recommendation:** ship the code as-is (it is correct and forward-compatible) rather than
contort the prompt down to an unusable ~12-word cap. Revisit once Ollama/the `gemma4` GGUF is
updated — re-run the same bisection (test files kept in this session's scratchpad pattern, see
§5) to confirm the fix before re-enabling reliance on it.

---

## 5. What's Next

| Item | Status |
|---|---|
| Image-diagnosis application code (entities/DTOs/service/prompt/UI) | ✅ Complete, verified independent of the vision issue |
| Image-diagnosis actually working end-to-end for real conversational messages | 🔲 Blocked on an Ollama/`gemma4` runtime fix (§4) |
| Voice-First Assistant (Bangla ASR/TTS) | 🔲 Still undecided — separate from this session's work |
| Re-verify vision once Ollama/model is updated | 🔲 Re-run the bisection test (short vs. long prompt + image, direct `/api/chat` call) to confirm the fix before relying on it in the app |

---

*Built with Java Spring Boot · MySQL · Ollama · AI-assisted with Claude Code*