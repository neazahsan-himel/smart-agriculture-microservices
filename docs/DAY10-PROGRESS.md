# Smart Agriculture Advisor — Day 10 Progress

> **Project:** Smart Agriculture Advisor
> **Stack:** Java 21 · Spring Boot 4.0.6 · Spring Cloud 2025.1.0 · MySQL 8.x · Ollama · Whisper/Piper
> **Date:** 2026-10-29
> **Approach:** AI-assisted development using Claude Code

---

## 1. What This Log Covers

Day 9 ended with two open items: the undecided Voice-First Assistant, and Day 7/8's carried-over
known issues. Both were worked on in commits made after Day 9 that never got a progress log. This
log catches up on those commits (§2–§4) and records today's cleanup session (§5–§7):

| Commit | Date | Summary |
|---|---|---|
| `09bfe4a` | 2026-09-07 | Fix `createdAt`/`updatedAt` null on POST/PUT responses (all services) |
| `0812f7f` | 2026-09-20 | Voice assistant support in `ai-advisor-service` |
| `bab7a1f` | 2026-09-20 | Voice input and playback in the Farmer Chat UI |
| `8a1a21e` | 2026-09-20 | Docker setup for local speech services + `docs/USER_MANUAL.md` |
| *(today)* | 2026-10-29 | Parser unit tests + bug fix, timezone issue closed, stray archetype removed |

---

## 2. Fixed: `createdAt`/`updatedAt` null on create/update responses (`09bfe4a`)

**Root cause:** inside a `@Transactional` service method, `repository.save()` only registers the
entity with the persistence context. The `INSERT`/`UPDATE` — and Hibernate's in-memory
`@CreationTimestamp`/`@UpdateTimestamp` generation — was deferred to commit, which happens *after*
the response DTO had already read the still-null fields.

**Fix:** every `save()` whose result feeds straight into a response mapper became
`saveAndFlush()`, so timestamps are populated before the DTO is built. Applied across
`farmer-service`, `crop-service`, `weather-service`, `farm-asset-service`, `notification-service`
and `ai-advisor-service` (6 files, 14 lines).

---

## 3. Voice-First Assistant (`0812f7f`, `bab7a1f`)

The "still undecided" item from Day 9 was resolved by supporting **both** options behind one
switch, so the project can run fully offline or use a cloud provider:

```properties
# provider: local (Docker Whisper ASR + Piper TTS) | cloud (Google)
speech.provider=local
speech.local.asr.base-url=http://localhost:9000
speech.local.tts.base-url=http://localhost:5002
speech.cloud.google.credentials-path=${GOOGLE_APPLICATION_CREDENTIALS:}
speech.language-code=bn-BD
```

**Backend (`ai-advisor-service`)**
- New `client/speech/` package with two interfaces, `SpeechToTextClient` and `TextToSpeechClient`,
  each with a local and a Google implementation selected via `@ConditionalOnProperty`
  (`local` is the default when the property is missing).
- `ConversationDto` requests accept an optional `audioBase64` (`@Size` max 10 MB); responses carry
  `replyAudioBase64`, and each message reports `hasAudio`.
- `Message` stores the farmer's original audio (`LONGTEXT`) alongside the text.
- Turn flow: if audio is present it is transcribed and used as the message. If the farmer
  **also** typed text, the typed text wins and a transcription failure is only logged — the turn
  still succeeds. If audio is the *only* input, an ASR failure fails the request. When the turn
  came in as audio, the reply text is synthesized back to speech.
- Google Cloud dependencies (`google-cloud-speech`, `google-cloud-texttospeech`) are versioned
  through the `libraries-bom` (26.48.0).

**Frontend (Chat UI)**
- Microphone button records with `MediaRecorder` (`audio/webm`), shows a preview bar with the
  recording duration and a remove button, and plays the spoken reply.
- Send is disabled while the recording is being staged, so a message can't be sent before the
  async `onstop` handler has attached the audio.
- Microphone permission errors are shown in the chat instead of failing silently.

---

## 4. Local Speech Services + User Manual (`8a1a21e`)

`docker-compose.yml` now runs the two local speech engines:

| Container | Image | Port | Notes |
|---|---|---|---|
| `whisper-asr` | `onerahmet/openai-whisper-asr-webservice:latest` | 9000 | `faster_whisper` engine, `medium` model |
| `piper-tts` | built from `docker/piper-tts/` (small Python server) | 5002 | `POST /tts` |

`docs/USER_MANUAL.md` was added as a plain-language explainer of the current system and a demo
walkthrough (deliberately describing what is built, not the PRD vision).

---

## 5. Today: Unit Tests for `AdvisoryResponseParser` — and a Bug They Found

Until today every service had only the generated `contextLoads()` test. The first real test
targets `AdvisoryResponseParser`, the piece that broke on Day 9, so that fix can't silently
regress.

**New file:** `ai-advisor-service/src/test/java/.../service/AdvisoryResponseParserTest.java` — a
plain JUnit 5 + AssertJ test (no Spring context, no Ollama, runs in under a second). 8 cases:

| Case | What it pins down |
|---|---|
| Well-formed trailing `---` block | The format the prompt asks for |
| llava shape #1 — no `---` separator | Day 9 §4 regression |
| llava shape #2 — tag block repeated | Day 9 §4 regression; prose kept, *last* values used |
| llava shape #3 — tag block before the prose | Day 9 §4 regression |
| Lowercase / indented tags | Case-insensitive parsing + stripping |
| No tags at all | Text returned as-is, safe defaults |
| Tag lines with invalid values | Safe defaults **and** no tag lines leaked (see below) |
| `null` / empty / blank reply | Empty text, safe defaults |

**Bug found:** the test for invalid tag values failed. When the model writes tag lines but *every*
value is unparseable (e.g. `CONFIDENCE: VERY_HIGH`, `RISK: unknown`), the parser decided whether to
strip based on finding a *valid* tag value — found none — and returned the raw reply, so the
farmer saw the `---` and `CONFIDENCE:`/`RISK:` lines in the chat.

**Fix** (`AdvisoryResponseParser.java`): decide whether to strip based on the presence of tag
*lines* (`TAG_LINE`) instead of valid tag values. Defaults still apply for the unparseable values.

**Result:** `Tests run: 8, Failures: 0, Errors: 0` (`mvn -o test -Dtest=AdvisoryResponseParserTest`).

> **Trade-off to know:** stripping already removed any line starting with `Risk:`/`Confidence:`
> whenever a valid tag was present. It now also does so in replies with no valid tags. A model
> sentence like `"Risk: the fungus may spread"` at the start of a line would be hidden — the same
> behaviour that already existed for every well-formed reply.

---

## 6. Closed: The "Timezone Display Quirk"

Carried as an open item since Day 7 §10. Re-checked today; it is **not an application bug** and
needs no code change:

- All services use `serverTimezone=UTC` in the JDBC URL, and entities use `LocalDateTime` with
  `@CreationTimestamp`/`@UpdateTimestamp`. Hibernate binds those in the JVM's zone and the MySQL
  driver converts them to UTC on write and back on read, so the app round-trips consistently.
- The only mismatch was the Git Bash shell's own `date` (BST, `+01:00`) versus the JVM and MySQL
  (`+06:00`) on the dev machine.
- **Practical rule (unchanged):** when inserting timestamps by hand in a SQL client, use
  `UTC_TIMESTAMP()`, not `NOW()`.

Status moved from "carried over" to **closed — environment note, not a defect**.

---

## 7. Cleanup: Removed the Unused Root Maven Archetype

The repo root contained `src/main/resources/archetype-resources/` (`App.java`, `AppTest.java`,
`archetype.xml`, an archetype `pom.xml`) — the untouched skeleton generated by
`maven-archetype-plugin` in the very first commit (`51533d0`). Nothing referenced it except
IntelliJ's auto-generated `.idea/compiler.xml`. It was removed with `git rm`, so it's recoverable
from history.

The root `pom.xml` (still the archetype's minimal pom, not a multi-module aggregator) was **left
in place** — IntelliJ may load it as the project root. Turning it into a real parent/aggregator is
a separate decision.

---

## 8. What's Next

| Item | Status |
|---|---|
| `createdAt`/`updatedAt: null` on POST/PUT | ✅ Fixed (`09bfe4a`) |
| Voice-First Assistant (Bangla ASR/TTS) | ✅ Built — local (Whisper/Piper) or Google, switchable |
| Timezone display quirk | ✅ Closed — environment-only, not a defect |
| Unit tests for `AdvisoryResponseParser` | ✅ 8 tests; found and fixed a tag-leak bug |
| Unit tests for the other services' service layers | 🔲 Not started — only `contextLoads()` exists |
| Pin `whisper-asr` image to a version instead of `:latest` | 🔲 Open — `:latest` can change underneath the project |
| Weather-to-Decision + Best-Time Recommendation (MVP Core) | 🔲 Proposed next feature — reuses `weather-service`, `FarmTask`, `FollowUpScheduler`, `notification-service` |

---

*Built with Java Spring Boot · MySQL · Ollama · AI-assisted with Claude Code*
