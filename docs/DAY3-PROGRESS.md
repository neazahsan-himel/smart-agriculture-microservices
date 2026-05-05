# Smart Agriculture Advisor — Day 3 Progress

> **Project:** Smart Agriculture Advisor
> **Stack:** Java 21 · Spring Boot 4.0.6 · Spring Cloud · MySQL · Eureka
> **Date:** 2026-05-05
> **Approach:** AI-assisted development using Claude Code

---

## Table of Contents

1. [What Was Built Today](#1-what-was-built-today)
2. [Architecture](#2-architecture)
3. [Folder Structure](#3-folder-structure)
4. [WeatherRecord Entity Design](#4-weatherrecord-entity-design)
5. [API Reference](#5-api-reference)
6. [Validation Rules](#6-validation-rules)
7. [Database Design](#7-database-design)
8. [Exception Handling](#8-exception-handling)
9. [How I Used Claude — Step-by-Step](#9-how-i-used-claude--step-by-step)
10. [How to Run](#10-how-to-run)
11. [Test with Postman](#11-test-with-postman)
12. [What's Next](#12-whats-next)

---

## 1. What Was Built Today

| # | Task | Status |
|---|------|--------|
| 1 | Read DAY1-PROGRESS and DAY2-PROGRESS to identify next service | ✅ Done |
| 2 | Reviewed existing weather-service scaffold (entity, DTO, repository, exceptions) | ✅ Done |
| 3 | Implemented `WeatherServiceImpl` with CRUD, duplicate checks, null-safe update | ✅ Done |
| 4 | Implemented `WeatherController` with versioned REST API, pagination, soft delete | ✅ Done |
| 5 | Updated `CLAUDE.md` — weather-service status: Planned → Done | ✅ Done |

---

## 2. Architecture

### Where weather-service fits

```
Client (Postman / Mobile App)
          │
          ▼
   [ api-gateway ]          ← port 8080 (future)
          │
          ▼
 [ service-registry ]       ← port 8761 (Eureka)
          │
    ┌─────┴──────────────────────────────────────┐
    │                  │                          │
[ farmer-service ]  [ crop-service ]   [ weather-service ]  ← NEW
   port 8081           port 8082           port 8083
   farmer_db           crop_db             weather_db
```

### What weather-service does

`weather-service` is a **weather data store** — it persists both observed and forecast weather records for locations worldwide.

Future services will query it:
- `ai-advisor-service` → "What is the current weather for this farmer's GPS location?"
- `crop-service` → cross-reference crop temperature/rainfall tolerances with live conditions
- `notification-service` → trigger alerts when weather deviates from crop tolerances

### Two record types

| Type | Meaning |
|---|---|
| `OBSERVATION` | Actual measured weather data (from a sensor, weather station, or API) |
| `FORECAST` | Predicted future weather (from a weather API or model) |

### Layered Architecture (inside weather-service)

```
HTTP Request
    │
    ▼
[ WeatherController ]     ← receives request, validates, calls service
    │
    ▼
[ WeatherServiceImpl ]    ← business logic, duplicate check, DTO ↔ entity
    │
    ▼
[ WeatherRepository ]     ← Spring Data JPA, soft-delete queries
    │
    ▼
[ MySQL — weather_db ]
```

---

## 3. Folder Structure

```
weather-service/
├── pom.xml
└── src/
    └── main/
        ├── java/com/smartagriculture/weatherservice/
        │   ├── WeatherServiceApplication.java
        │   ├── controller/
        │   │   └── WeatherController.java         ← NEW (Day 3)
        │   ├── service/
        │   │   ├── WeatherService.java            ← interface
        │   │   └── WeatherServiceImpl.java        ← NEW (Day 3)
        │   ├── repository/
        │   │   └── WeatherRepository.java
        │   ├── entity/
        │   │   └── WeatherRecord.java
        │   ├── dto/
        │   │   └── WeatherDto.java
        │   └── exception/
        │       ├── ResourceNotFoundException.java
        │       ├── DuplicateResourceException.java
        │       └── GlobalExceptionHandler.java
        └── resources/
            └── application.properties
```

---

## 4. WeatherRecord Entity Design

### Why these fields?

The `WeatherRecord` entity is designed as an **AI-ready weather data store**. Both historical observations and short-range forecasts live in the same table, differentiated by `recordType`.

#### Identity Fields

| Field | Type | Why |
|---|---|---|
| `id` | `String` (UUID) | Safe for distributed systems |
| `countryCode` | `String` (2 chars) | ISO 3166-1 alpha-2 — enables country-level filtering |
| `region` | `String` (nullable) | Sub-national targeting; null = country-wide |
| `latitude` / `longitude` | `Double` (nullable) | Exact GPS coordinate for sensor-level precision |

#### Observation Time

| Field | Type | Why |
|---|---|---|
| `recordedAt` | `LocalDateTime` | The time the weather data applies to (not when it was inserted) |
| `recordType` | `Enum` | `OBSERVATION` = real data; `FORECAST` = predicted |

#### Weather Measurements (AI Core Fields)

| Field | Why |
|---|---|
| `temperatureCelsius` | AI matches against `crop.minTemperatureCelsius` / `maxTemperatureCelsius` |
| `feelsLikeCelsius` | Better represents conditions for outdoor farm work decisions |
| `humidityPercent` | High humidity → fungal disease risk; AI uses for disease alerts |
| `rainfallMm` | AI compares to `crop.minRainfallMm` / `maxRainfallMm` for irrigation decisions |
| `windSpeedKmh` | Affects spray timing (pesticide/herbicide drift) |
| `windDirection` | Wind direction matters for fire risk and cross-field drift |
| `weatherCondition` | Enum overview (SUNNY, RAINY, THUNDERSTORM…) for human-readable advice |
| `visibilityKm` | Farm machinery safety threshold |
| `uvIndex` | Farmer health alerts in tropical regions |
| `dataSource` | Tracks origin: `MANUAL`, `OPENWEATHER`, `SENSOR` — important for AI data quality weighting |

#### Lifecycle Fields

| Field | Why |
|---|---|
| `status` | `ACTIVE` / `INACTIVE` — deactivate stale records without deleting |
| `deleted` | Soft delete — never lose weather history |
| `createdAt` / `updatedAt` | Audit trail |
| `version` | Optimistic locking |

### Duplicate Rule

A weather record is unique on: **`countryCode + region + recordedAt + recordType`**

- Two forecasts for the same place and time = duplicate
- An observation and a forecast for the same place and time = allowed (different `recordType`)
- `NULL` region is handled at the service layer (MySQL treats `NULL != NULL` in unique indexes)

---

## 5. API Reference

**Base URL:** `http://localhost:8083/api/v1`

---

### POST `/api/v1/weather-records`
Add a new weather record.

**Request Body:**
```json
{
  "countryCode": "BD",
  "region": "Rajshahi",
  "latitude": 24.4103,
  "longitude": 88.9799,
  "recordedAt": "2026-05-05T06:00:00",
  "recordType": "OBSERVATION",
  "temperatureCelsius": 28.5,
  "feelsLikeCelsius": 31.0,
  "humidityPercent": 72,
  "rainfallMm": 0.0,
  "windSpeedKmh": 12.0,
  "windDirection": "SW",
  "weatherCondition": "PARTLY_CLOUDY",
  "visibilityKm": 10.0,
  "uvIndex": 7,
  "dataSource": "OPENWEATHER"
}
```

**Response `201 Created`:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "countryCode": "BD",
  "region": "Rajshahi",
  "latitude": 24.4103,
  "longitude": 88.9799,
  "recordedAt": "2026-05-05T06:00:00",
  "recordType": "OBSERVATION",
  "temperatureCelsius": 28.5,
  "humidityPercent": 72,
  "rainfallMm": 0.0,
  "weatherCondition": "PARTLY_CLOUDY",
  "status": "ACTIVE",
  "createdAt": "2026-05-05T10:00:00",
  "updatedAt": "2026-05-05T10:00:00"
}
```

> **Duplicate rule:** `countryCode + region + recordedAt + recordType` must be unique. Sending a duplicate returns `409 Conflict`.

---

### GET `/api/v1/weather-records`
Get all weather records (paginated).

**Query Parameters:**

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | Page number (0-indexed) |
| `size` | `20` | Records per page (max 100) |
| `sortBy` | `recordedAt` | Field to sort by |
| `sortDir` | `desc` | `asc` or `desc` |

**Example:** `GET /api/v1/weather-records?page=0&size=10&sortBy=recordedAt&sortDir=desc`

**Response `200 OK`:**
```json
{
  "content": [ { ... }, { ... } ],
  "totalElements": 5000,
  "totalPages": 500,
  "number": 0,
  "size": 10
}
```

---

### GET `/api/v1/weather-records/{id}`
Get a single weather record by UUID.

**Response `200 OK`:** Returns `WeatherDto.Response`

**Response `404 Not Found`:**
```json
{
  "timestamp": "2026-05-05T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Weather record not found with id: abc-123"
}
```

---

### PUT `/api/v1/weather-records/{id}`
Update an existing weather record. Only measurement fields are null-safe; `countryCode`, `recordedAt`, and `recordType` are always required.

**Request Body:** Same as POST

**Response `200 OK`:** Returns updated `WeatherDto.Response`

---

### DELETE `/api/v1/weather-records/{id}`
Soft-delete a weather record (sets `deleted=true`, `status=INACTIVE`).

**Response `200 OK`:**
```json
"Weather record deleted successfully"
```

> **Note:** Records are never physically removed. Weather history is preserved for AI model training.

---

## 6. Validation Rules

| Field | Rule |
|---|---|
| `countryCode` | Required. Exactly 2 characters (e.g. `BD`, `IN`, `US`) |
| `recordedAt` | Required. ISO-8601 datetime |
| `recordType` | Required. `OBSERVATION` or `FORECAST` |
| `latitude` | Between `-90.0` and `90.0` |
| `longitude` | Between `-180.0` and `180.0` |
| `temperatureCelsius` | Between `-90.0` and `60.0` |
| `feelsLikeCelsius` | Between `-90.0` and `60.0` |
| `humidityPercent` | Between `0` and `100` |
| `rainfallMm` | Zero or positive |
| `windSpeedKmh` | Zero or positive |
| `visibilityKm` | Zero or positive |
| `uvIndex` | Between `0` and `20` |

**Validation error response `400 Bad Request`:**
```json
{
  "timestamp": "2026-05-05T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "countryCode": "Country code must be exactly 2 characters (e.g. BD, IN)",
    "recordType": "Record type is required (OBSERVATION or FORECAST)"
  }
}
```

**Duplicate error response `409 Conflict`:**
```json
{
  "timestamp": "2026-05-05T10:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Weather record already exists for: BD / Rajshahi at 2026-05-05T06:00"
}
```

---

## 7. Database Design

**Database:** `weather_db`
**Table:** `weather_records`

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `id` | `VARCHAR(36)` | PK | UUID auto-generated |
| `country_code` | `CHAR(2)` | NOT NULL | ISO 3166-1 alpha-2 |
| `region` | `VARCHAR(255)` | — | Nullable — null = country-wide |
| `latitude` | `DOUBLE` | — | GPS coordinate |
| `longitude` | `DOUBLE` | — | GPS coordinate |
| `recorded_at` | `DATETIME` | NOT NULL | Time this weather applies to |
| `record_type` | `VARCHAR(20)` | NOT NULL | ENUM: OBSERVATION, FORECAST |
| `temperature_celsius` | `DOUBLE` | — | Air temperature |
| `feels_like_celsius` | `DOUBLE` | — | Apparent temperature |
| `humidity_percent` | `INT` | — | 0–100 % |
| `rainfall_mm` | `DOUBLE` | — | Precipitation in period |
| `wind_speed_kmh` | `DOUBLE` | — | Wind speed |
| `wind_direction` | `VARCHAR(20)` | — | ENUM: N, NE, E, SE, S, SW, W, NW |
| `weather_condition` | `VARCHAR(20)` | — | ENUM: SUNNY, RAINY... |
| `visibility_km` | `DOUBLE` | — | Visibility in km |
| `uv_index` | `INT` | — | 0–20 scale |
| `data_source` | `VARCHAR(255)` | — | Origin: OPENWEATHER, SENSOR... |
| `status` | `VARCHAR(20)` | NOT NULL | Default: ACTIVE |
| `deleted` | `BOOLEAN` | NOT NULL | Default: false |
| `created_at` | `DATETIME` | — | Auto-set on insert |
| `updated_at` | `DATETIME` | — | Auto-set on update |
| `version` | `BIGINT` | — | Optimistic locking |

**Indexes:**
```sql
INDEX idx_weather_country    (country_code)
INDEX idx_weather_region     (region)
INDEX idx_weather_recordedAt (recorded_at)
INDEX idx_weather_type       (record_type)
INDEX idx_weather_condition  (weather_condition)
INDEX idx_weather_status     (status)
INDEX idx_weather_deleted    (deleted)
```

> **Why no unique index on (countryCode, region, recordedAt, recordType)?**
> MySQL treats `NULL != NULL` in unique indexes, so two country-wide records (null region) with the same time and type would both pass the constraint. Uniqueness is enforced at the **service layer** where null is handled correctly.

---

## 8. Exception Handling

All exceptions are handled centrally in `GlobalExceptionHandler.java`.

| Exception | HTTP Status | Trigger |
|---|---|---|
| `DuplicateResourceException` | `409 Conflict` | Same (countryCode + region + recordedAt + recordType) already exists |
| `ResourceNotFoundException` | `404 Not Found` | ID does not exist or is soft-deleted |
| `MethodArgumentNotValidException` | `400 Bad Request` | Validation annotation fails |
| `Exception` (generic) | `500 Internal Server Error` | Unexpected runtime error |

---

## 9. How I Used Claude — Step-by-Step

**Step 1 — Orient Claude with progress history**

> *"I am continuing my Smart Agriculture Advisor project. See CLAUDE.md for project rules. Today I want to build weather-service. Follow the same architecture as crop-service. I also have a file called DAY1-PROGRESS and DAY2-PROGRESS — read this and do what to do next."*

Claude read both progress files and identified that DAY2 ended with crop-service complete and weather-service as the next service to build.

---

**Step 2 — Audit existing scaffold**

Claude read all existing weather-service files:
- `pom.xml`, `application.properties` — already complete
- `WeatherRecord.java`, `WeatherDto.java`, `WeatherRepository.java` — already complete
- `WeatherService.java` (interface), exception classes — already complete

**Identified:** Only `WeatherServiceImpl.java` and `WeatherController.java` were missing.

---

**Step 3 — Implement WeatherServiceImpl**

Claude implemented the service following CLAUDE.md rules:
- Duplicate check on create: `countryCode + region + recordedAt + recordType`
- Duplicate check on update: same check but excludes the current record by `id`
- Null-safe update: `countryCode`, `recordedAt`, `recordType` are always set (they are `@NotNull`/`@NotBlank`); all measurement fields only overwrite when non-null
- Soft delete: sets `deleted = true` and `status = INACTIVE`
- `toResponse()` private mapper: clean entity → DTO conversion

---

**Step 4 — Implement WeatherController**

Claude implemented the controller following CLAUDE.md rules:
- Versioned base path: `/api/v1/weather-records`
- `POST` returns `201 Created`
- `GET` (list) accepts `page`, `size` (max 100), `sortBy`, `sortDir` params with defaults
- `PUT` updates and returns updated record
- `DELETE` returns `200 OK` with `"Weather record deleted successfully"` string

---

**What worked well with Claude:**
- Precisely identified which files existed vs. which were missing before writing any code
- Applied CLAUDE.md architecture rules consistently without needing to re-explain them
- Correctly handled the null-region duplicate check at the service layer (not the DB layer)
- Matched the exact patterns from farmer-service and crop-service

---

## 10. How to Run

### Prerequisites
- Java 21
- Maven 3.9+
- MySQL 8.x running locally
- `service-registry` running on port `8761`

### Step 1 — Create the database
```sql
CREATE DATABASE weather_db;
```

### Step 2 — Ensure service-registry is running
```bash
cd service-registry
mvn spring-boot:run
```
Eureka dashboard: http://localhost:8761

### Step 3 — Start weather-service
```bash
cd weather-service
mvn spring-boot:run
```
Service starts at: http://localhost:8083

### Step 4 — Verify Eureka registration
Open http://localhost:8761 — you should see `FARMER-SERVICE`, `CROP-SERVICE`, and `WEATHER-SERVICE` all listed.

---

## 11. Test with Postman

### Create a Weather Observation
```
POST http://localhost:8083/api/v1/weather-records
Content-Type: application/json

{
  "countryCode": "BD",
  "region": "Rajshahi",
  "latitude": 24.4103,
  "longitude": 88.9799,
  "recordedAt": "2026-05-05T06:00:00",
  "recordType": "OBSERVATION",
  "temperatureCelsius": 28.5,
  "feelsLikeCelsius": 31.0,
  "humidityPercent": 72,
  "rainfallMm": 0.0,
  "windSpeedKmh": 12.0,
  "windDirection": "SW",
  "weatherCondition": "PARTLY_CLOUDY",
  "visibilityKm": 10.0,
  "uvIndex": 7,
  "dataSource": "OPENWEATHER"
}
```

### Create a Forecast (same location, different type — allowed)
```
POST http://localhost:8083/api/v1/weather-records
Content-Type: application/json

{
  "countryCode": "BD",
  "region": "Rajshahi",
  "recordedAt": "2026-05-06T06:00:00",
  "recordType": "FORECAST",
  "temperatureCelsius": 31.0,
  "humidityPercent": 65,
  "rainfallMm": 5.0,
  "weatherCondition": "RAINY",
  "dataSource": "OPENWEATHER"
}
```

### Test Duplicate Protection
```
POST http://localhost:8083/api/v1/weather-records
(same body as the first POST above)

→ Expected: 409 Conflict
→ "Weather record already exists for: BD / Rajshahi at 2026-05-05T06:00"
```

### Get All Weather Records (paginated)
```
GET http://localhost:8083/api/v1/weather-records?page=0&size=10&sortBy=recordedAt&sortDir=desc
```

### Get Record by ID
```
GET http://localhost:8083/api/v1/weather-records/{id}
```

### Partial Update (only update temperature and humidity)
```
PUT http://localhost:8083/api/v1/weather-records/{id}
Content-Type: application/json

{
  "countryCode": "BD",
  "recordedAt": "2026-05-05T06:00:00",
  "recordType": "OBSERVATION",
  "temperatureCelsius": 29.0,
  "humidityPercent": 75
}
```

### Soft Delete
```
DELETE http://localhost:8083/api/v1/weather-records/{id}

→ Expected: 200 OK — "Weather record deleted successfully"
→ Record stays in DB with deleted=true, status=INACTIVE
```

---

## 12. What's Next

| Service | Status |
|---|---|
| `service-registry` | ✅ Complete |
| `farmer-service` | ✅ Complete |
| `crop-service` | ✅ Complete |
| `weather-service` | ✅ Complete |
| `ai-advisor-service` | 🔲 Next |
| `notification-service` | 🔲 Planned |
| `api-gateway` | 🔲 Planned |

---

*Built with Java Spring Boot · AI-assisted with Claude Code*