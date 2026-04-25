# Smart Agriculture Advisor — Day 1 Progress

> **Project:** Smart Agriculture Advisor  
> **Stack:** Java 21 · Spring Boot 4.x · Spring Cloud · MySQL · Eureka  
> **Date:** 2026-04-25  
> **Approach:** AI-assisted development using Claude Code

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [What Was Built Today](#2-what-was-built-today)
3. [Architecture](#3-architecture)
4. [Folder Structure](#4-folder-structure)
5. [API Reference](#5-api-reference)
6. [Validation Rules](#6-validation-rules)
7. [Database Design](#7-database-design)
8. [Exception Handling](#8-exception-handling)
9. [How I Used Claude — Step-by-Step](#9-how-i-used-claude--step-by-step)
10. [How to Run](#10-how-to-run)
11. [Test with Postman](#11-test-with-postman)

---

## 1. Project Overview

**Smart Agriculture Advisor** is a microservices-based platform designed to help farmers worldwide get intelligent advice about their crops, weather, soil conditions, and farming practices.

The long-term vision includes:
- AI-powered crop and soil recommendations
- Voice-based interaction (for farmers with low digital literacy)
- Image analysis for crop disease detection
- Multi-language support for global farmers
- Mobile-first design for low-bandwidth environments

This documentation covers **Day 1** — the initial microservice setup and the complete `farmer-service` implementation.

---

## 2. What Was Built Today

| # | Task | Status |
|---|------|--------|
| 1 | Initialized microservices project structure | ✅ Done |
| 2 | Set up `service-registry` (Eureka Server) on port `8761` | ✅ Done |
| 3 | Created `farmer-service` on port `8081` | ✅ Done |
| 4 | Connected `farmer-service` to Eureka for service discovery | ✅ Done |
| 5 | Designed scalable `Farmer` entity with AI-ready fields | ✅ Done |
| 6 | Created `FarmerDto` (split into `Request` / `Response`) | ✅ Done |
| 7 | Implemented `FarmerRepository` with soft-delete queries | ✅ Done |
| 8 | Implemented `FarmerService` interface + `FarmerServiceImpl` | ✅ Done |
| 9 | Built REST APIs: Create, Get All, Get By ID, Update, Delete | ✅ Done |
| 10 | Added input validation with Jakarta Validation | ✅ Done |
| 11 | Added `GlobalExceptionHandler` with structured error responses | ✅ Done |
| 12 | Connected MySQL database (`farmer_db`) | ✅ Done |
| 13 | Upgraded entity design for global scale (UUID, soft delete, audit) | ✅ Done |
| 14 | Tested all endpoints using Postman | ✅ Done |

---

## 3. Architecture

### Microservices Overview

```
Client (Postman / Mobile App / Browser)
          │
          ▼
   [ api-gateway ]  ← port 8080 (Spring Cloud Gateway — future)
          │
          ▼
 [ service-registry ]  ← port 8761 (Netflix Eureka Server)
          │
    ┌─────┴──────────────────────────┐
    │                                │
[ farmer-service ]           [ other services ]
   port 8081                  (crop, weather, AI...)
    │
    ▼
 [ MySQL ]
 farmer_db
```

### Layered Architecture (inside farmer-service)

```
HTTP Request
    │
    ▼
[ Controller ]      ← receives request, calls service, returns response
    │
    ▼
[ Service ]         ← business logic, DTO ↔ entity mapping
    │
    ▼
[ Repository ]      ← Spring Data JPA, database queries
    │
    ▼
[ MySQL Database ]
```

**Key architectural decisions:**

| Decision | Reason |
|---|---|
| UUID primary key | Safe for distributed systems; Long IDs leak record counts |
| Soft delete (`deleted=true`) | Never lose data; required for audit trails and analytics |
| DTO split (Request / Response) | Client cannot inject `id` or audit fields; cleaner API contract |
| Versioned API (`/api/v1/`) | Allows future breaking changes without disrupting existing clients |
| Paginated GET all | Prevents full-table reads on millions of records |
| `@Version` optimistic locking | Prevents race conditions on concurrent updates |

---

## 4. Folder Structure

```
farmer-service/
├── pom.xml
└── src/
    └── main/
        ├── java/com/smartagriculture/farmerservice/
        │   ├── FarmerServiceApplication.java
        │   ├── controller/
        │   │   └── FarmerController.java
        │   ├── service/
        │   │   ├── FarmerService.java          ← interface
        │   │   └── FarmerServiceImpl.java      ← implementation
        │   ├── repository/
        │   │   └── FarmerRepository.java
        │   ├── entity/
        │   │   └── Farmer.java                 ← JPA entity with enums
        │   ├── dto/
        │   │   └── FarmerDto.java              ← nested Request + Response
        │   └── exception/
        │       ├── ResourceNotFoundException.java
        │       └── GlobalExceptionHandler.java
        └── resources/
            └── application.properties
```

---

## 5. API Reference

**Base URL:** `http://localhost:8081/api/v1`

---

### POST `/api/v1/farmers`
Create a new farmer.

**Request Body:**
```json
{
  "name": "Rahim Uddin",
  "phoneNumber": "+8801712345678",
  "email": "rahim@example.com",
  "countryCode": "BD",
  "region": "Rajshahi",
  "district": "Natore",
  "latitude": 24.4103,
  "longitude": 88.9799,
  "farmSizeHectares": 3.5,
  "farmType": "RICE",
  "soilType": "LOAMY",
  "irrigationMethod": "FLOOD",
  "preferredLanguage": "bn",
  "timezone": "Asia/Dhaka",
  "deviceType": "SMARTPHONE",
  "aiConsentGiven": true
}
```

**Response `201 Created`:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Rahim Uddin",
  "phoneNumber": "+8801712345678",
  "countryCode": "BD",
  "region": "Rajshahi",
  "farmSizeHectares": 3.5,
  "farmType": "RICE",
  "status": "ACTIVE",
  "aiConsentGiven": true,
  "createdAt": "2026-04-25T10:00:00",
  "updatedAt": "2026-04-25T10:00:00"
}
```

---

### GET `/api/v1/farmers`
Get all farmers (paginated).

**Query Parameters:**

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | Page number (0-indexed) |
| `size` | `20` | Records per page (max 100) |
| `sortBy` | `createdAt` | Field to sort by |
| `sortDir` | `desc` | `asc` or `desc` |

**Example:** `GET /api/v1/farmers?page=0&size=10&sortBy=name&sortDir=asc`

**Response `200 OK`:**
```json
{
  "content": [ { ... }, { ... } ],
  "totalElements": 1500,
  "totalPages": 150,
  "number": 0,
  "size": 10
}
```

---

### GET `/api/v1/farmers/{id}`
Get a single farmer by UUID.

**Response `200 OK`:** Returns `FarmerDto.Response`

**Response `404 Not Found`:**
```json
{
  "timestamp": "2026-04-25T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Farmer not found with id: abc-123"
}
```

---

### PUT `/api/v1/farmers/{id}`
Update an existing farmer.

**Request Body:** Same as POST (all fields optional except `name`)

**Response `200 OK`:** Returns updated `FarmerDto.Response`

---

### DELETE `/api/v1/farmers/{id}`
Soft-delete a farmer (sets `deleted=true`, `status=INACTIVE`).

**Response `200 OK`:**
```json
"Farmer deleted successfully"
```

> **Note:** Records are never physically removed from the database. This preserves audit history and supports data recovery.

---

## 6. Validation Rules

| Field | Rule |
|---|---|
| `name` | Required. 2–100 characters |
| `phoneNumber` | E.164 format: `+[country code][number]` e.g. `+8801712345678` |
| `email` | Valid email format if provided |
| `countryCode` | Exactly 2 characters (ISO 3166-1 alpha-2: `BD`, `IN`, `US`) |
| `latitude` | Between `-90.0` and `90.0` |
| `longitude` | Between `-180.0` and `180.0` |
| `farmSizeHectares` | Must be a positive number |
| `preferredLanguage` | Max 5 characters (ISO 639-1: `en`, `bn`, `hi`) |

**Validation error response `400 Bad Request`:**
```json
{
  "timestamp": "2026-04-25T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "name": "Farmer name is required",
    "phoneNumber": "Phone number must be in E.164 format (e.g. +8801712345678)",
    "countryCode": "Country code must be exactly 2 characters"
  }
}
```

---

## 7. Database Design

**Database:** `farmer_db`  
**Table:** `farmers`

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `id` | `VARCHAR(36)` | PK | UUID auto-generated |
| `name` | `VARCHAR(255)` | NOT NULL | Required |
| `phone_number` | `VARCHAR(20)` | UNIQUE | E.164 format |
| `email` | `VARCHAR(255)` | — | Optional |
| `country_code` | `CHAR(2)` | — | ISO 3166-1 alpha-2 |
| `region` | `VARCHAR(255)` | — | State/Province |
| `district` | `VARCHAR(255)` | — | Sub-region |
| `latitude` | `DOUBLE` | — | GPS coordinate |
| `longitude` | `DOUBLE` | — | GPS coordinate |
| `farm_size_hectares` | `DOUBLE` | — | Farm area |
| `farm_type` | `VARCHAR(20)` | — | ENUM: RICE, WHEAT... |
| `soil_type` | `VARCHAR(20)` | — | ENUM: CLAY, LOAMY... |
| `irrigation_method` | `VARCHAR(20)` | — | ENUM: DRIP, FLOOD... |
| `preferred_language` | `VARCHAR(5)` | — | ISO 639-1 |
| `timezone` | `VARCHAR(50)` | — | IANA timezone |
| `device_type` | `VARCHAR(20)` | — | ENUM: SMARTPHONE... |
| `ai_consent_given` | `BOOLEAN` | NOT NULL | Default: false |
| `status` | `VARCHAR(20)` | NOT NULL | Default: ACTIVE |
| `deleted` | `BOOLEAN` | NOT NULL | Default: false |
| `created_at` | `DATETIME` | — | Auto-set on insert |
| `updated_at` | `DATETIME` | — | Auto-set on update |
| `version` | `BIGINT` | — | Optimistic locking |

**Indexes:**
```sql
INDEX idx_farmer_phone   (phone_number)
INDEX idx_farmer_country (country_code)
INDEX idx_farmer_region  (region)
INDEX idx_farmer_status  (status)
INDEX idx_farmer_deleted (deleted)
```

---

## 8. Exception Handling

All exceptions are handled centrally in `GlobalExceptionHandler.java` using `@RestControllerAdvice`.

| Exception | HTTP Status | Trigger |
|---|---|---|
| `ResourceNotFoundException` | `404 Not Found` | ID does not exist or is soft-deleted |
| `MethodArgumentNotValidException` | `400 Bad Request` | Validation annotation fails |
| `Exception` (generic) | `500 Internal Server Error` | Unexpected runtime error |

All error responses share a consistent structure:
```json
{
  "timestamp": "...",
  "status": 404,
  "error": "Not Found",
  "message": "Human-readable message"
}
```

---

## 9. How I Used Claude — Step-by-Step

This project was built using **Claude Code** as an AI pair programmer. Below are the exact prompts used at each step.

---

**Step 1 — Explore the project structure**

> *"GO TO service-registry"*

Claude read the existing `ServiceRegistryApplication.java` and `application.properties`, confirmed it was correctly set up as a Eureka server, and identified what was missing before moving to farmer-service.

---

**Step 2 — Build the complete farmer-service**

> *"You are a senior Java Spring Boot developer. Build a complete farmer-service microservice with:*
> *- Java 17+, Spring Boot, Spring Data JPA, MySQL, Eureka, Lombok, Validation*
> *- Port 8081, connected to Eureka at http://localhost:8761/eureka*
> *- Farmer entity: id, name, location, farmSize*
> *- CRUD APIs: POST /api/farmers, GET /api/farmers, GET /api/farmers/{id}, DELETE /api/farmers/{id}*
> *- GlobalExceptionHandler with ResourceNotFoundException*
> *- application.properties config"*

Claude generated all layers in one shot:
- Updated `pom.xml` with JPA, MySQL, Eureka, Lombok, Validation
- Created `Farmer` entity, `FarmerDto`, `FarmerRepository`, `FarmerService`, `FarmerServiceImpl`, `FarmerController`
- Created `ResourceNotFoundException` and `GlobalExceptionHandler`
- Wrote full `application.properties`

---

**Step 3 — Add the update API**

> *"Add update API to my farmer-service.*
> *- PUT /api/farmers/{id}*
> *- update existing farmer*
> *- use DTO*
> *- validate input*
> *- handle not found exception"*

Claude added `updateFarmer()` to the service interface and implementation, and added the `PUT /{id}` endpoint to the controller — without touching any other existing code.

---

**Step 4 — Improve for global scale**

> *"You are a senior software architect with experience building global-scale systems (millions of users).*
> *Guide me: How should I design farmer-service for scalability? What fields should I include for AI? How to design for multiple countries and languages? What should I avoid?"*

Claude acted as a system architect and:
- Replaced `Long id` with `UUID`
- Added geo fields (`latitude`, `longitude`, `countryCode`, `region`)
- Added AI-readiness fields (`soilType`, `irrigationMethod`, `preferredLanguage`, `aiConsentGiven`)
- Added lifecycle fields (`status`, `deleted`, audit timestamps, `@Version`)
- Added database indexes for all filter columns
- Split `FarmerDto` into `Request` and `Response`
- Upgraded to versioned URL `/api/v1/farmers`
- Added paginated `GET /api/v1/farmers` with page/size/sort params
- Configured HikariCP connection pool in `application.properties`
- Explained event-driven AI integration strategy (Kafka, no direct REST coupling)

---

**What worked well with Claude:**
- Generating complete, production-ready boilerplate in seconds
- Getting architectural guidance with real trade-off reasoning
- Modifying only the files that needed changing (no unnecessary rewrites)
- Explaining *why* each decision was made, not just *what* to write

---

## 10. How to Run

### Prerequisites
- Java 21
- Maven 3.9+
- MySQL 8.x running locally
- (Optional) Eureka service-registry running

### Step 1 — Create the database
```sql
CREATE DATABASE farmer_db;
```

### Step 2 — Start service-registry
```bash
cd service-registry
mvn spring-boot:run
```
Eureka dashboard: [http://localhost:8761](http://localhost:8761)

### Step 3 — Start farmer-service
```bash
cd farmer-service
mvn spring-boot:run
```
Service starts at: [http://localhost:8081](http://localhost:8081)

### Step 4 — Verify Eureka registration
Open [http://localhost:8761](http://localhost:8761) — you should see `FARMER-SERVICE` listed as a registered instance.

---

## 11. Test with Postman

### Create a Farmer
```
POST http://localhost:8081/api/v1/farmers
Content-Type: application/json

{
  "name": "Rahim Uddin",
  "phoneNumber": "+8801712345678",
  "countryCode": "BD",
  "region": "Rajshahi",
  "farmSizeHectares": 3.5,
  "farmType": "RICE",
  "soilType": "LOAMY",
  "preferredLanguage": "bn",
  "timezone": "Asia/Dhaka",
  "deviceType": "SMARTPHONE",
  "aiConsentGiven": true
}
```

### Get All Farmers (paginated)
```
GET http://localhost:8081/api/v1/farmers?page=0&size=10
```

### Get Farmer by ID
```
GET http://localhost:8081/api/v1/farmers/{id}
```

### Update a Farmer
```
PUT http://localhost:8081/api/v1/farmers/{id}
Content-Type: application/json

{
  "name": "Rahim Uddin Updated",
  "farmSizeHectares": 5.0,
  "soilType": "CLAY"
}
```

### Delete a Farmer (soft delete)
```
DELETE http://localhost:8081/api/v1/farmers/{id}
```

---

## What's Next

| Service | Status |
|---|---|
| `service-registry` | ✅ Complete |
| `farmer-service` | ✅ Complete |
| `crop-service` | 🔲 Next |
| `weather-service` | 🔲 Planned |
| `ai-advisor-service` | 🔲 Planned |
| `notification-service` | 🔲 Planned |
| `api-gateway` | 🔲 Planned |

---

*Built with Java Spring Boot · AI-assisted with Claude Code*