# Smart Agriculture Advisor — Day 4 Progress

> **Project:** Smart Agriculture Advisor
> **Stack:** Java 21 · Spring Boot 4.0.6 · Spring Cloud · Eureka · OpenFeign · Claude API
> **Date:** 2026-05-05
> **Approach:** AI-assisted development using Claude Code

---

## Table of Contents

1. [What Was Built Today](#1-what-was-built-today)
2. [Architecture](#2-architecture)
3. [Folder Structure](#3-folder-structure)
4. [Design Decisions](#4-design-decisions)
5. [API Reference](#5-api-reference)
6. [Validation Rules](#6-validation-rules)
7. [Exception Handling](#7-exception-handling)
8. [How I Used Claude — Step-by-Step](#8-how-i-used-claude--step-by-step)
9. [How to Run](#9-how-to-run)
10. [Test with Postman](#10-test-with-postman)
11. [What's Next](#11-whats-next)

---

## 1. What Was Built Today

| # | Task | Status |
|---|------|--------|
| 1 | Read DAY1–DAY3 progress to identify next service | ✅ Done |
| 2 | Audited ai-advisor-service scaffold (only main class + broken pom.xml) | ✅ Done |
| 3 | Rewrote `pom.xml` — Spring Boot 4.0.6, Spring Cloud 2025.1.0, OpenFeign, no JPA/MySQL | ✅ Done |
| 4 | Configured `application.properties` — port 8084, Eureka, Claude API config | ✅ Done |
| 5 | Updated `AiAdvisorServiceApplication.java` — added `@EnableFeignClients` | ✅ Done |
| 6 | Created `AppConfig.java` — `RestTemplate` bean for Claude API calls | ✅ Done |
| 7 | Created external DTOs: `FarmerResponse`, `CropSummary`, `WeatherSummary`, `PageResponse<T>` | ✅ Done |
| 8 | Created `AdvisorDto` with `Request` and `Response` inner classes + `QueryType` enum | ✅ Done |
| 9 | Created 3 Feign clients: `FarmerServiceClient`, `CropServiceClient`, `WeatherServiceClient` | ✅ Done |
| 10 | Created `ClaudeApiClient` — calls Anthropic API, parses structured response | ✅ Done |
| 11 | Created `AiAdvisorServiceImpl` — orchestrates context + Claude API + response | ✅ Done |
| 12 | Created `AiAdvisorController` — POST `/api/v1/advisor/advice` | ✅ Done |
| 13 | Created `GlobalExceptionHandler` — 400, 503, 500 | ✅ Done |
| 14 | Verified clean `mvn compile` — BUILD SUCCESS | ✅ Done |
| 15 | Updated `CLAUDE.md` — ai-advisor-service status: Planned → Done | ✅ Done |

---

## 2. Architecture

### Where ai-advisor-service fits

```
Client (Postman / Mobile App)
          │
          ▼
   [ api-gateway ]          ← port 8080 (future)
          │
          ▼
 [ service-registry ]       ← port 8761 (Eureka)
          │
    ┌─────┴────────────────────────────────────────────┐
    │               │               │                  │
[ farmer-service ] [ crop-service ] [ weather-service ] [ ai-advisor-service ]  ← NEW
   port 8081         port 8082        port 8083           port 8084
   farmer_db         crop_db          weather_db          (no DB)
                                                              │
                                                              ▼
                                                    [ Claude API (Anthropic) ]
```

### What ai-advisor-service does

`ai-advisor-service` is the **AI brain** of the system. It:
1. Receives a farmer's question and query type via REST
2. Calls `farmer-service` to fetch the farmer's profile (soil type, location, farm size)
3. Calls `crop-service` to fetch the crop catalog
4. Calls `weather-service` to fetch the latest weather observations/forecasts
5. Builds a rich context prompt from all three data sources
6. Calls **Claude API** (claude-sonnet-4-6) to generate expert agricultural advice
7. Returns the AI-generated advice as a structured JSON response

### Key design choices

| Decision | Reason |
|---|---|
| No database | Advice is generated on demand — no need to persist AI responses |
| OpenFeign for inter-service calls | Declarative HTTP client, auto-integrates with Eureka service discovery |
| Graceful degradation | If farmer/crop/weather service is down, advice is still generated with whatever data is available |
| RestTemplate for Claude API | Feign is for internal services; external HTTPS API needs plain RestTemplate |
| `PageResponse<T>` wrapper | Spring's `Page<T>` is not Feign-deserializable; custom wrapper maps the JSON content correctly |

### Layered Architecture

```
HTTP Request
    │
    ▼
[ AiAdvisorController ]      ← validates request, calls service
    │
    ▼
[ AiAdvisorServiceImpl ]     ← fetches context from 3 services, builds prompt
    │               │
    │         [ ClaudeApiClient ]  ← calls Anthropic API, parses response
    │
    ├── [ FarmerServiceClient ]    ← Feign → farmer-service
    ├── [ CropServiceClient ]      ← Feign → crop-service
    └── [ WeatherServiceClient ]   ← Feign → weather-service
```

---

## 3. Folder Structure

```
ai-advisor-service/
├── pom.xml
└── src/
    └── main/
        ├── java/com/smartagriculture/aiadvisorservice/
        │   ├── AiAdvisorServiceApplication.java     ← @SpringBootApplication @EnableFeignClients
        │   ├── config/
        │   │   └── AppConfig.java                  ← RestTemplate bean
        │   ├── client/
        │   │   ├── FarmerServiceClient.java         ← Feign → farmer-service
        │   │   ├── CropServiceClient.java           ← Feign → crop-service
        │   │   ├── WeatherServiceClient.java        ← Feign → weather-service
        │   │   └── ClaudeApiClient.java             ← HTTP → Anthropic API
        │   ├── dto/
        │   │   ├── AdvisorDto.java                  ← Request + Response + QueryType
        │   │   └── external/
        │   │       ├── FarmerResponse.java
        │   │       ├── CropSummary.java
        │   │       ├── WeatherSummary.java
        │   │       └── PageResponse.java            ← generic page wrapper
        │   ├── service/
        │   │   ├── AiAdvisorService.java            ← interface
        │   │   └── AiAdvisorServiceImpl.java        ← implementation
        │   ├── controller/
        │   │   └── AiAdvisorController.java
        │   └── exception/
        │       ├── ExternalServiceException.java    ← 503
        │       └── GlobalExceptionHandler.java
        └── resources/
            └── application.properties
```

---

## 4. Design Decisions

### QueryType enum

The `queryType` field tells Claude what kind of advice to optimise for:

| QueryType | Use Case |
|---|---|
| `CROP_RECOMMENDATION` | "What crops should I grow this season?" |
| `WEATHER_ALERT` | "Is a storm coming? What should I do?" |
| `PEST_CONTROL` | "My tomatoes have spots — what's the problem?" |
| `PLANTING_SCHEDULE` | "When should I plant rice this year?" |
| `IRRIGATION_ADVICE` | "How much water does my crop need?" |
| `GENERAL` | Any other farming question |

### Graceful degradation

If `farmer-service`, `crop-service`, or `weather-service` is unavailable:
- The service logs a warning and continues
- The prompt is built with available data only
- Claude still generates advice (general advice without personalisation)
- Only the `ClaudeApiClient` throws an error — advice cannot be generated without the AI

### System prompt

Claude is given a fixed system prompt defining its persona and response format:
- Expert agricultural advisor for South Asian smallholder farming
- Responses must include: situation assessment + 2-4 bullet recommendations + warnings
- Language must be simple and practical

### Prompt structure

The user message sent to Claude is structured in 4 sections:
```
Query Type: CROP_RECOMMENDATION

== FARMER PROFILE ==
Name: Abdul Rahman
Location: Rajshahi, BD
Soil Type: LOAMY
Farm Size: 2.5 hectares

== AVAILABLE CROPS IN CATALOG ==
- Rice (Basmati) | Type: CEREAL | Season: KHARIF | Soil: LOAMY | Temp: 20–35°C | Duration: 120 days
- Wheat | Type: CEREAL | Season: RABI | Soil: CLAY | Temp: 5–25°C | Duration: 90 days
...

== RECENT WEATHER CONDITIONS ==
- OBSERVATION at 2026-05-05T06:00 | PARTLY_CLOUDY | Temp: 28.5°C | Humidity: 72% | Rainfall: 0.0mm | Location: Rajshahi, BD
...

== FARMER'S QUESTION ==
What crop should I plant this month for the best yield?
```

---

## 5. API Reference

**Base URL:** `http://localhost:8084/api/v1`

---

### POST `/api/v1/advisor/advice`
Get AI-powered agricultural advice.

**Request Body:**
```json
{
  "question": "What crop should I plant this month for the best yield?",
  "queryType": "CROP_RECOMMENDATION",
  "farmerId": "550e8400-e29b-41d4-a716-446655440000",
  "countryCode": "BD",
  "region": "Rajshahi"
}
```

| Field | Required | Description |
|---|---|---|
| `question` | **Yes** | The farmer's question (max 1000 chars) |
| `queryType` | **Yes** | One of: `CROP_RECOMMENDATION`, `WEATHER_ALERT`, `PEST_CONTROL`, `PLANTING_SCHEDULE`, `IRRIGATION_ADVICE`, `GENERAL` |
| `farmerId` | No | If provided, fetches farmer profile from farmer-service |
| `countryCode` | No | 2-char ISO code — used if no farmerId, or as override |
| `region` | No | Sub-national region — used for location context |

**Response `201 Created`:**
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "question": "What crop should I plant this month for the best yield?",
  "queryType": "CROP_RECOMMENDATION",
  "advice": "Based on your location in Rajshahi, Bangladesh with loamy soil and the current temperature of 28.5°C, here is my recommendation:\n\n**Situation:** May is the start of the Kharif (monsoon) season...",
  "farmerId": "550e8400-e29b-41d4-a716-446655440000",
  "countryCode": "BD",
  "region": "Rajshahi",
  "generatedAt": "2026-05-05T10:30:00"
}
```

---

## 6. Validation Rules

| Field | Rule |
|---|---|
| `question` | Required. Max 1000 characters |
| `queryType` | Required. Must be a valid `QueryType` enum value |
| `countryCode` | Optional. If provided, must be exactly 2 characters |

**Validation error `400 Bad Request`:**
```json
{
  "timestamp": "2026-05-05T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "question": "Question is required",
    "queryType": "Query type is required"
  }
}
```

---

## 7. Exception Handling

| Exception | HTTP Status | Trigger |
|---|---|---|
| `ExternalServiceException` | `503 Service Unavailable` | Claude API call fails |
| `MethodArgumentNotValidException` | `400 Bad Request` | Validation annotation fails |
| `Exception` (generic) | `500 Internal Server Error` | Unexpected runtime error |

> **Note:** Feign failures for farmer/crop/weather services do NOT throw exceptions to the client — they are caught internally and advice is generated with whatever context is available.

---

## 8. How I Used Claude — Step-by-Step

**Step 1 — Orient Claude with progress history**

> *"I am continuing my Smart Agriculture Advisor project. See CLAUDE.md for project rules. Today I want to build ai-advisor-service (next service per DAY3-PROGRESS). Follow the same architecture as weather-service."*

Claude read all three progress files and confirmed that `ai-advisor-service` was next.

---

**Step 2 — Audit existing scaffold**

Claude read all existing ai-advisor-service files:
- `pom.xml` — broken (wrong Spring Boot version 4.0.0, wrong artifact IDs, missing dependencies)
- `application.properties` — empty (only had `spring.application.name`)
- `AiAdvisorServiceApplication.java` — minimal, missing `@EnableFeignClients`

**Identified:** Everything needed to be built from scratch (18 files total).

---

**Step 3 — Plan architecture**

Key architectural decisions made by Claude:
- No JPA/MySQL in pom.xml — this service has no DB
- Use **OpenFeign** for calling farmer/crop/weather services via Eureka
- Use **RestTemplate** for Claude API (external HTTPS, no Eureka)
- `PageResponse<T>` DTO to handle paginated responses from other services
- Graceful degradation: catch Feign exceptions, continue with partial context

---

**Step 4 — Implement all 18 files**

Claude implemented in dependency order:
1. `pom.xml` → `application.properties` → main class
2. External DTOs (`PageResponse`, `FarmerResponse`, `CropSummary`, `WeatherSummary`)
3. `AdvisorDto` with `Request`, `Response`, `QueryType`
4. 3 Feign clients + `ClaudeApiClient`
5. `AiAdvisorService` interface + `AiAdvisorServiceImpl`
6. `AiAdvisorController`
7. `ExternalServiceException` + `GlobalExceptionHandler`

---

**Step 5 — Verify build**

```
mvn compile → BUILD SUCCESS
```

---

**What worked well with Claude:**
- Correctly identified no DB was needed — didn't include JPA/MySQL
- Properly handled the `PageResponse<T>` generic deserialization issue with `@JsonIgnoreProperties`
- Implemented graceful fallback: if downstream services are unavailable, AI advice still flows
- Structured the Claude prompt clearly with labelled sections for maximum AI context quality
- Applied CLAUDE.md architecture rules consistently (validation in DTOs only, 201 for POST, versioned paths)

---

## 9. How to Run

### Prerequisites
- Java 21
- Maven 3.9+
- `service-registry` running on port `8761`
- Set environment variable: `ANTHROPIC_API_KEY=your_key_here`

> Optional but recommended: also run `farmer-service`, `crop-service`, `weather-service` for full context.

### Step 1 — Ensure service-registry is running
```bash
cd service-registry
mvn spring-boot:run
```

### Step 2 — Set Claude API key
```bash
# Windows
set ANTHROPIC_API_KEY=sk-ant-...

# Mac/Linux
export ANTHROPIC_API_KEY=sk-ant-...
```

### Step 3 — Start ai-advisor-service
```bash
cd ai-advisor-service
mvn spring-boot:run
```
Service starts at: http://localhost:8084

### Step 4 — Verify Eureka registration
Open http://localhost:8761 — you should see `AI-ADVISOR-SERVICE` listed alongside the other services.

---

## 10. Test with Postman

### Get Crop Recommendation (with farmer profile)
```
POST http://localhost:8084/api/v1/advisor/advice
Content-Type: application/json

{
  "question": "What crop should I plant this month to get the best yield on my farm?",
  "queryType": "CROP_RECOMMENDATION",
  "farmerId": "<id from farmer-service>",
  "countryCode": "BD",
  "region": "Rajshahi"
}
```

### Get Weather-Based Alert
```
POST http://localhost:8084/api/v1/advisor/advice
Content-Type: application/json

{
  "question": "We have high humidity and recent rainfall. Should I worry about disease on my rice crop?",
  "queryType": "WEATHER_ALERT",
  "countryCode": "BD",
  "region": "Rajshahi"
}
```

### Planting Schedule
```
POST http://localhost:8084/api/v1/advisor/advice
Content-Type: application/json

{
  "question": "When is the best time to plant wheat in this region?",
  "queryType": "PLANTING_SCHEDULE",
  "countryCode": "BD",
  "region": "Dhaka"
}
```

### General Question (no location)
```
POST http://localhost:8084/api/v1/advisor/advice
Content-Type: application/json

{
  "question": "What are the best practices for organic farming?",
  "queryType": "GENERAL"
}
```

### Test Validation (missing required field)
```
POST http://localhost:8084/api/v1/advisor/advice
Content-Type: application/json

{}

→ Expected: 400 Bad Request
→ fieldErrors: { "question": "Question is required", "queryType": "Query type is required" }
```

---

## 11. What's Next

| Service | Status |
|---|---|
| `service-registry` | ✅ Complete |
| `farmer-service` | ✅ Complete |
| `crop-service` | ✅ Complete |
| `weather-service` | ✅ Complete |
| `ai-advisor-service` | ✅ Complete |
| `notification-service` | 🔲 Next |
| `api-gateway` | 🔲 Planned |

---

*Built with Java Spring Boot · AI-assisted with Claude Code*