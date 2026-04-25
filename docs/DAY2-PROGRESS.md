# Smart Agriculture Advisor — Day 2 Progress

> **Project:** Smart Agriculture Advisor
> **Stack:** Java 21 · Spring Boot 4.0.6 · Spring Cloud · MySQL · Eureka
> **Date:** 2026-04-25
> **Approach:** AI-assisted development using Claude Code

---

## Table of Contents

1. [What Was Built Today](#1-what-was-built-today)
2. [Architecture](#2-architecture)
3. [Folder Structure](#3-folder-structure)
4. [Crop Entity Design](#4-crop-entity-design)
5. [API Reference](#5-api-reference)
6. [Validation Rules](#6-validation-rules)
7. [Database Design](#7-database-design)
8. [Exception Handling](#8-exception-handling)
9. [Bug Fixes Applied to farmer-service](#9-bug-fixes-applied-to-farmer-service)
10. [How to Run](#10-how-to-run)
11. [Test with Postman](#11-test-with-postman)
12. [What's Next](#12-whats-next)

---

## 1. What Was Built Today

| # | Task | Status |
|---|------|--------|
| 1 | Fixed 3 bugs in `farmer-service` from Day 1 | ✅ Done |
| 2 | Created `CLAUDE.md` with project-wide architecture rules | ✅ Done |
| 3 | Updated `crop-service` `pom.xml` — fixed Spring Boot version, added all dependencies | ✅ Done |
| 4 | Configured `application.properties` for port `8082`, `crop_db`, Eureka, HikariCP | ✅ Done |
| 5 | Designed `Crop` entity with AI-ready growing-condition fields | ✅ Done |
| 6 | Created `CropDto` (split into `Request` / `Response`) | ✅ Done |
| 7 | Implemented `CropRepository` with soft-delete + null-safe duplicate queries | ✅ Done |
| 8 | Implemented `CropService` interface + `CropServiceImpl` | ✅ Done |
| 9 | Built REST APIs: Create, Get All (paginated), Get By ID, Update, Delete | ✅ Done |
| 10 | Added input validation with Jakarta Validation | ✅ Done |
| 11 | Added `GlobalExceptionHandler` with structured error responses | ✅ Done |
| 12 | Connected MySQL database (`crop_db`) | ✅ Done |

---

## 2. Architecture

### Where crop-service fits

```
Client (Postman / Mobile App)
          │
          ▼
   [ api-gateway ]          ← port 8080 (future)
          │
          ▼
 [ service-registry ]       ← port 8761 (Eureka)
          │
    ┌─────┴──────────────────────────┐
    │                                │
[ farmer-service ]          [ crop-service ]       ← NEW
   port 8081                  port 8082
   farmer_db                  crop_db
```

### What crop-service does

`crop-service` is a **crop catalog** — a reference library of crops with their growing conditions.

Future services will query it:
- `ai-advisor-service` → "What crops grow in LOAMY soil at 25°C with 1200mm rainfall?"
- `farmer-service` → link a farmer's current crop to an entry in this catalog
- `weather-service` → cross-reference crop temperature tolerance with forecast data

### Layered Architecture (inside crop-service)

```
HTTP Request
    │
    ▼
[ Controller ]      ← receives request, calls service, returns response
    │
    ▼
[ Service ]         ← business logic, duplicate check, DTO ↔ entity mapping
    │
    ▼
[ Repository ]      ← Spring Data JPA, soft-delete queries
    │
    ▼
[ MySQL — crop_db ]
```

---

## 3. Folder Structure

```
crop-service/
├── pom.xml
└── src/
    └── main/
        ├── java/com/smartagriculture/cropservice/
        │   ├── CropServiceApplication.java
        │   ├── controller/
        │   │   └── CropController.java
        │   ├── service/
        │   │   ├── CropService.java            ← interface
        │   │   └── CropServiceImpl.java        ← implementation
        │   ├── repository/
        │   │   └── CropRepository.java
        │   ├── entity/
        │   │   └── Crop.java                   ← JPA entity with enums
        │   ├── dto/
        │   │   └── CropDto.java                ← nested Request + Response
        │   └── exception/
        │       ├── ResourceNotFoundException.java
        │       ├── DuplicateResourceException.java
        │       └── GlobalExceptionHandler.java
        └── resources/
            └── application.properties
```

---

## 4. Crop Entity Design

### Why these fields?

The `Crop` entity is designed as an **AI-ready crop catalog**. Every field has a specific purpose:

#### Identity Fields

| Field | Type | Why |
|---|---|---|
| `id` | `String` (UUID) | Safe for distributed systems; no sequential ID leaks |
| `name` | `String` | The crop name e.g. "Rice", "Wheat", "Tomato" |
| `variety` | `String` (nullable) | Same crop has many varieties — "Rice (Basmati)" vs "Rice (IR-64)" have very different growing conditions |
| `description` | `String` (500 chars) | Human-readable info for farmers; shown in the mobile app |

#### Classification Fields

| Field | Enum Values | Why |
|---|---|---|
| `cropType` | `CEREAL, LEGUME, VEGETABLE, FRUIT, OILSEED, FIBER, SPICE, OTHER` | AI uses this to group and filter recommendations by category |
| `season` | `KHARIF, RABI, ZAID, YEAR_ROUND` | Tells the AI *when* a crop can be grown — prevents wrong-season advice |

> **KHARIF** = monsoon/summer crops (June–November) e.g. Rice, Maize
> **RABI** = winter crops (November–April) e.g. Wheat, Mustard
> **ZAID** = short summer crops between Rabi and Kharif e.g. Cucumber, Pumpkin

#### Growing Conditions (AI Core Fields)

These are the most important fields — the AI uses them to match crops to a farmer's location and environment:

| Field | Why |
|---|---|
| `idealSoilType` | Matches against `farmer.soilType` — AI won't suggest rice to a farmer with sandy soil |
| `minTemperatureCelsius` | Lower bound for healthy growth — prevents cold-damage crop suggestions |
| `maxTemperatureCelsius` | Upper bound — prevents heat-stress crop suggestions |
| `minRainfallMm` | Minimum annual rainfall needed — critical for rain-fed farming decisions |
| `maxRainfallMm` | Too much rain damages some crops (e.g. wheat gets fungal disease) |
| `growingDurationDays` | Seed-to-harvest days — AI schedules planting calendar and alerts |
| `irrigationRequired` | If `true`, AI will only recommend this crop to farmers who have irrigation infrastructure |
| `typicalYieldPerHectare` | Tonnes/hectare — used for profit estimation and planning advice |

#### Geography Fields

| Field | Why |
|---|---|
| `countryCode` | Some crops are region-specific (e.g. Jute → `BD`, Quinoa → `PE`); AI filters by country |
| `region` | Sub-national targeting for hyperlocal advice |

#### Lifecycle Fields (same as farmer-service)

| Field | Why |
|---|---|
| `status` | `ACTIVE` / `INACTIVE` — deactivate outdated crop entries without deleting |
| `deleted` | Soft delete — never lose catalog data; supports audit and recovery |
| `createdAt` / `updatedAt` | Audit trail |
| `version` | Optimistic locking — prevents two admins overwriting each other simultaneously |

---

## 5. API Reference

**Base URL:** `http://localhost:8082/api/v1`

---

### POST `/api/v1/crops`
Add a new crop to the catalog.

**Request Body:**
```json
{
  "name": "Rice",
  "variety": "Basmati",
  "description": "Long-grain aromatic rice, premium quality",
  "cropType": "CEREAL",
  "season": "KHARIF",
  "idealSoilType": "LOAMY",
  "minTemperatureCelsius": 20.0,
  "maxTemperatureCelsius": 35.0,
  "minRainfallMm": 1000.0,
  "maxRainfallMm": 2000.0,
  "growingDurationDays": 120,
  "irrigationRequired": true,
  "typicalYieldPerHectare": 4.5,
  "countryCode": "BD",
  "region": "Rajshahi"
}
```

**Response `201 Created`:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Rice",
  "variety": "Basmati",
  "cropType": "CEREAL",
  "season": "KHARIF",
  "idealSoilType": "LOAMY",
  "minTemperatureCelsius": 20.0,
  "maxTemperatureCelsius": 35.0,
  "growingDurationDays": 120,
  "irrigationRequired": true,
  "typicalYieldPerHectare": 4.5,
  "status": "ACTIVE",
  "createdAt": "2026-04-25T10:00:00",
  "updatedAt": "2026-04-25T10:00:00"
}
```

> **Duplicate rule:** `name + variety` must be unique. "Rice (Basmati)" and "Rice (IR-64)" are two different catalog entries. "Rice" with no variety is a third. Sending a duplicate returns `409 Conflict`.

---

### GET `/api/v1/crops`
Get all crops (paginated).

**Query Parameters:**

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | Page number (0-indexed) |
| `size` | `20` | Records per page (max 100) |
| `sortBy` | `createdAt` | Field to sort by |
| `sortDir` | `desc` | `asc` or `desc` |

**Example:** `GET /api/v1/crops?page=0&size=10&sortBy=name&sortDir=asc`

**Response `200 OK`:**
```json
{
  "content": [ { ... }, { ... } ],
  "totalElements": 250,
  "totalPages": 25,
  "number": 0,
  "size": 10
}
```

---

### GET `/api/v1/crops/{id}`
Get a single crop by UUID.

**Response `200 OK`:** Returns `CropDto.Response`

**Response `404 Not Found`:**
```json
{
  "timestamp": "2026-04-25T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Crop not found with id: abc-123"
}
```

---

### PUT `/api/v1/crops/{id}`
Update an existing crop. Only fields you provide are updated — omitted fields keep their current value.

**Request Body:** Same as POST (only `name` is required; all others optional)

**Response `200 OK`:** Returns updated `CropDto.Response`

---

### DELETE `/api/v1/crops/{id}`
Soft-delete a crop (sets `deleted=true`, `status=INACTIVE`).

**Response `200 OK`:**
```json
"Crop deleted successfully"
```

> **Note:** Records are never physically removed. This preserves catalog history.

---

## 6. Validation Rules

| Field | Rule |
|---|---|
| `name` | Required. 2–100 characters |
| `variety` | Optional. Max 100 characters |
| `description` | Optional. Max 500 characters |
| `countryCode` | Optional. Exactly 2 characters (e.g. `BD`, `IN`, `US`) |
| `minTemperatureCelsius` | Between `-50.0` and `60.0` |
| `maxTemperatureCelsius` | Between `-50.0` and `60.0` |
| `minRainfallMm` | Zero or positive |
| `maxRainfallMm` | Zero or positive |
| `growingDurationDays` | Positive integer |
| `typicalYieldPerHectare` | Positive number |

**Validation error response `400 Bad Request`:**
```json
{
  "timestamp": "2026-04-25T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "name": "Crop name is required",
    "countryCode": "Country code must be exactly 2 characters (e.g. BD, IN)"
  }
}
```

**Duplicate error response `409 Conflict`:**
```json
{
  "timestamp": "2026-04-25T10:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Crop already exists: Rice (Basmati)"
}
```

---

## 7. Database Design

**Database:** `crop_db`
**Table:** `crops`

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `id` | `VARCHAR(36)` | PK | UUID auto-generated |
| `name` | `VARCHAR(255)` | NOT NULL | Required |
| `variety` | `VARCHAR(100)` | — | Nullable — null = generic |
| `description` | `VARCHAR(500)` | — | Optional |
| `crop_type` | `VARCHAR(20)` | — | ENUM: CEREAL, LEGUME... |
| `season` | `VARCHAR(20)` | — | ENUM: KHARIF, RABI... |
| `ideal_soil_type` | `VARCHAR(20)` | — | ENUM: CLAY, LOAMY... |
| `min_temperature_celsius` | `DOUBLE` | — | Growing condition |
| `max_temperature_celsius` | `DOUBLE` | — | Growing condition |
| `min_rainfall_mm` | `DOUBLE` | — | Annual min rainfall |
| `max_rainfall_mm` | `DOUBLE` | — | Annual max rainfall |
| `growing_duration_days` | `INT` | — | Seed-to-harvest days |
| `irrigation_required` | `BOOLEAN` | — | Water infrastructure needed |
| `typical_yield_per_hectare` | `DOUBLE` | — | Tonnes/hectare |
| `country_code` | `CHAR(2)` | — | ISO 3166-1 alpha-2 |
| `region` | `VARCHAR(255)` | — | Sub-national area |
| `status` | `VARCHAR(20)` | NOT NULL | Default: ACTIVE |
| `deleted` | `BOOLEAN` | NOT NULL | Default: false |
| `created_at` | `DATETIME` | — | Auto-set on insert |
| `updated_at` | `DATETIME` | — | Auto-set on update |
| `version` | `BIGINT` | — | Optimistic locking |

**Indexes:**
```sql
INDEX idx_crop_type    (crop_type)
INDEX idx_crop_season  (season)
INDEX idx_crop_country (country_code)
INDEX idx_crop_status  (status)
INDEX idx_crop_deleted (deleted)
```

> **Why no unique index on (name, variety)?**
> MySQL treats `NULL != NULL` in unique indexes, so two rows with the same name and `NULL` variety would both be allowed. Uniqueness is enforced at the **service layer** instead, where null variety is handled correctly.

---

## 8. Exception Handling

All exceptions are handled centrally in `GlobalExceptionHandler.java`.

| Exception | HTTP Status | Trigger |
|---|---|---|
| `DuplicateResourceException` | `409 Conflict` | Same (name + variety) already exists |
| `ResourceNotFoundException` | `404 Not Found` | ID does not exist or is soft-deleted |
| `MethodArgumentNotValidException` | `400 Bad Request` | Validation annotation fails |
| `Exception` (generic) | `500 Internal Server Error` | Unexpected runtime error |

---

## 9. Bug Fixes Applied to farmer-service

Before building crop-service, three bugs discovered in Day 1's `farmer-service` were fixed:

### Bug 1 — Duplicate phone number returned 500 instead of 409
`FarmerRepository` had `existsByPhoneNumberAndDeletedFalse()` defined but it was never called. A second farmer with the same phone number hit the database unique constraint and threw an unhandled exception.

**Fix:** Added `DuplicateResourceException` class, added `409` handler in `GlobalExceptionHandler`, and added duplicate check in both `createFarmer()` and `updateFarmer()`.

### Bug 2 — `@NotBlank` on the JPA entity was dead code
`Farmer.java` had `@NotBlank(message = "Farmer name is required")` on the `name` field. Bean Validation on JPA entities is not triggered from the controller request flow — it was the DTO's `@NotBlank` doing the real work. The entity annotation was misleading.

**Fix:** Removed `@NotBlank` and its import from `Farmer.java`. The `@Column(nullable = false)` remains as the DB-level constraint.

### Bug 3 — PUT wiped optional fields with null
`updateFarmer()` blindly copied every field from the request to the entity, including nulls. Sending `{"name": "X"}` would null out `phoneNumber`, `region`, `soilType`, and all other optional fields.

**Fix:** `updateFarmer()` now uses null-safe assignment — a field is only updated when the request value is non-null. Only `name` is always set (it is required by `@NotBlank`).

---

## 10. How to Run

### Prerequisites
- Java 21
- Maven 3.9+
- MySQL 8.x running locally
- `service-registry` running on port `8761`

### Step 1 — Create the database
```sql
CREATE DATABASE crop_db;
```

### Step 2 — Ensure service-registry is running
```bash
cd service-registry
mvn spring-boot:run
```
Eureka dashboard: http://localhost:8761

### Step 3 — Start crop-service
```bash
cd crop-service
mvn spring-boot:run
```
Service starts at: http://localhost:8082

### Step 4 — Verify Eureka registration
Open http://localhost:8761 — you should see both `FARMER-SERVICE` and `CROP-SERVICE` listed.

---

## 11. Test with Postman

### Create a Crop
```
POST http://localhost:8082/api/v1/crops
Content-Type: application/json

{
  "name": "Rice",
  "variety": "Basmati",
  "description": "Long-grain aromatic rice grown in northern Bangladesh",
  "cropType": "CEREAL",
  "season": "KHARIF",
  "idealSoilType": "LOAMY",
  "minTemperatureCelsius": 20.0,
  "maxTemperatureCelsius": 35.0,
  "minRainfallMm": 1000.0,
  "maxRainfallMm": 2000.0,
  "growingDurationDays": 120,
  "irrigationRequired": true,
  "typicalYieldPerHectare": 4.5,
  "countryCode": "BD",
  "region": "Rajshahi"
}
```

### Test Duplicate Protection
```
POST http://localhost:8082/api/v1/crops
(same body as above)

→ Expected: 409 Conflict
→ "Crop already exists: Rice (Basmati)"
```

### Get All Crops (paginated)
```
GET http://localhost:8082/api/v1/crops?page=0&size=10&sortBy=name&sortDir=asc
```

### Get Crop by ID
```
GET http://localhost:8082/api/v1/crops/{id}
```

### Partial Update (only changes growingDurationDays — other fields unchanged)
```
PUT http://localhost:8082/api/v1/crops/{id}
Content-Type: application/json

{
  "name": "Rice",
  "growingDurationDays": 130
}
```

### Soft Delete
```
DELETE http://localhost:8082/api/v1/crops/{id}

→ Expected: 200 OK — "Crop deleted successfully"
→ Record stays in DB with deleted=true, status=INACTIVE
```

---

## 12. What's Next

| Service | Status |
|---|---|
| `service-registry` | ✅ Complete |
| `farmer-service` | ✅ Complete |
| `crop-service` | ✅ Complete |
| `weather-service` | 🔲 Next |
| `ai-advisor-service` | 🔲 Planned |
| `notification-service` | 🔲 Planned |
| `api-gateway` | 🔲 Planned |

---

*Built with Java Spring Boot · AI-assisted with Claude Code*