# AI Advisor Service

A **stateless orchestration microservice** that collects context from three internal services, builds a structured prompt, and returns AI-generated agricultural advice via the Google Gemini API.

- **Port:** `8084`
- **No database** — purely stateless
- **Registered with:** Netflix Eureka (`service-registry:8761`)

---

## Package Structure

```
ai-advisor-service/
├── AiAdvisorServiceApplication.java
│
├── client/
│   ├── GeminiApiClient.java             ← HTTP to Google Gemini API (RestTemplate)
│   ├── FarmerServiceClient.java         ← Feign → farmer-service  (port 8081)
│   ├── CropServiceClient.java           ← Feign → crop-service    (port 8082)
│   └── WeatherServiceClient.java        ← Feign → weather-service (port 8083)
│
├── config/
│   └── AppConfig.java                   ← RestTemplate bean
│
├── controller/
│   └── AiAdvisorController.java         ← POST /api/v1/advisor/advice
│
├── dto/
│   ├── AdvisorDto.java                  ← Request + Response + QueryType enum
│   └── external/
│       ├── FarmerResponse.java
│       ├── CropSummary.java
│       ├── WeatherSummary.java
│       └── PageResponse<T>.java
│
├── exception/
│   ├── ExternalServiceException.java
│   └── GlobalExceptionHandler.java
│
└── service/
    ├── AiAdvisorService.java            ← Interface
    └── AiAdvisorServiceImpl.java        ← Business logic
```

---

## Request Flow

```
Client
  │
  ▼  POST /api/v1/advisor/advice
AiAdvisorController
  │
  ▼
AiAdvisorServiceImpl.getAdvice()
  │
  ├── fetchFarmer(farmerId)    →  FarmerServiceClient  →  farmer-service:8081
  ├── fetchCrops()             →  CropServiceClient    →  crop-service:8082
  └── fetchWeather()           →  WeatherServiceClient →  weather-service:8083
  │
  ▼  buildUserMessage()  — assembles context into a structured text prompt
  │
  ▼
GeminiApiClient.getAdvice(systemPrompt, userMessage)
  │   POST https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent
  │
  ▼
AdvisorDto.Response  { id, question, queryType, advice, farmerId, countryCode, region, generatedAt }
```

---

## API Contract

### POST `/api/v1/advisor/advice`

**Request body:**

```json
{
  "question":    "Which crop should I plant this monsoon?",
  "queryType":   "CROP_RECOMMENDATION",
  "farmerId":    "uuid-of-farmer",
  "countryCode": "BD",
  "region":      "Dhaka"
}
```

| Field | Required | Notes |
|---|---|---|
| `question` | Yes | Max 1000 characters |
| `queryType` | Yes | See enum values below |
| `farmerId` | No | When provided, enriches context with farmer profile |
| `countryCode` | No | 2-char ISO code (e.g. `BD`, `IN`). Falls back to farmer's if omitted |
| `region` | No | Falls back to farmer's if omitted |

**QueryType enum values:**

| Value | Purpose |
|---|---|
| `CROP_RECOMMENDATION` | Which crops to plant |
| `WEATHER_ALERT` | Weather-based risk warnings |
| `PEST_CONTROL` | Pest and disease management |
| `PLANTING_SCHEDULE` | When to plant/harvest |
| `IRRIGATION_ADVICE` | Watering guidance |
| `GENERAL` | Any other farming question |

**Response — 201 Created:**

```json
{
  "id":           "random-uuid",
  "question":     "Which crop should I plant this monsoon?",
  "queryType":    "CROP_RECOMMENDATION",
  "advice":       "Based on your clay soil in Dhaka...\n• Plant Aman Rice...\n• Avoid...",
  "farmerId":     "uuid-of-farmer",
  "countryCode":  "BD",
  "region":       "Dhaka",
  "generatedAt":  "2026-05-07T10:30:00"
}
```

---

## HTTP Clients: Two Clients, Two Purposes

| Client | Technology | Used For | Why |
|---|---|---|---|
| `FarmerServiceClient` | OpenFeign | Internal service calls | Resolves service name via Eureka — no hard-coded URLs |
| `CropServiceClient` | OpenFeign | Internal service calls | Same as above |
| `WeatherServiceClient` | OpenFeign | Internal service calls | Same as above |
| `GeminiApiClient` | RestTemplate | Google Gemini external API | External API with `x-goog-api-key` header — not discoverable via Eureka, so Feign is not appropriate |

---

## Prompt Construction

`buildUserMessage()` assembles four sections into one text block sent to Gemini:

```
== FARMER PROFILE ==
Name, location, soil type, farm size
(falls back to request fields if no farmerId)

== AVAILABLE CROPS IN CATALOG ==
Up to 15 crops: name, variety, type, season, soil, temp range, duration

== RECENT WEATHER CONDITIONS ==
Latest 5 records: type, condition, temperature, humidity, rainfall, location

== FARMER'S QUESTION ==
The user's actual question
```

This block is the `user` message. A hardcoded system prompt defines the model's role as an agricultural advisor focused on South Asia (Bangladesh, India, Pakistan).

---

## Graceful Degradation

All three internal service fetchers (`fetchFarmer`, `fetchCrops`, `fetchWeather`) catch exceptions silently and return `null` / empty list. This means:

- If `farmer-service` is down → advice still works, prompt says "No farmer profile linked"
- If `crop-service` is down → advice still works, prompt says "No crop data available"
- If `weather-service` is down → advice still works, prompt says "No weather data available"
- If **Gemini API** fails → request fails with **503 Service Unavailable**

---

## Exception Handling

| Exception | HTTP Status | Scenario |
|---|---|---|
| `ExternalServiceException` | 503 Service Unavailable | Gemini API unreachable or empty response |
| `MethodArgumentNotValidException` | 400 Bad Request | Blank question, null queryType, invalid countryCode length |
| `Exception` (catch-all) | 500 Internal Server Error | Anything unexpected |

All handled centrally in `GlobalExceptionHandler` (`@RestControllerAdvice`).

Error response shape:

```json
{
  "timestamp": "2026-05-07T10:30:00",
  "status":    503,
  "error":     "Service Unavailable",
  "message":   "Failed to get advice from Gemini API: ..."
}
```

Validation errors additionally include:

```json
{
  "fieldErrors": {
    "question":   "Question is required",
    "queryType":  "Query type is required"
  }
}
```

---

## Configuration (`application.properties`)

```properties
server.port=8084
spring.application.name=ai-advisor-service

eureka.client.service-url.defaultZone=http://localhost:8761/eureka
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.prefer-ip-address=true

gemini.api.key=${GEMINI_API_KEY:your-gemini-api-key-here}
gemini.api.model=gemini-1.5-flash
gemini.api.max-tokens=1024
```

Set the real API key via environment variable:

```bash
export GEMINI_API_KEY=AIza...
```

---

## Dependencies (`pom.xml`)

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST controller, embedded Tomcat |
| `spring-boot-starter-validation` | `@Valid`, `@NotBlank`, `@Size` on DTOs |
| `spring-cloud-starter-netflix-eureka-client` | Service registration and discovery |
| `spring-cloud-starter-openfeign` | Declarative HTTP clients for internal services |
| `lombok` | Boilerplate reduction (`@Data`, `@Builder`, `@Slf4j`, etc.) |

No JPA or database driver — this service has no persistence layer.

---

## Notable Design Choices

- **Stateless by design** — no DB, no cache. The response `id` is a random UUID generated per request.
- **`queryType` is a prompt hint** — it is passed to Claude in the prompt but does not branch the Java logic. Claude uses it to focus the answer.
- **`countryCode` / `region` resolution priority** — request field wins, then farmer profile, then `null`.
- **Response code is 201** — consistent with the project's `POST` convention even though no resource is persisted.