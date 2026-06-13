# Smart Agriculture Advisor — Day 5 Progress

> **Project:** Smart Agriculture Advisor
> **Stack:** Java 21 · Spring Boot 4.0.6 · Spring Cloud · MySQL · Eureka
> **Date:** 2026-06-13
> **Approach:** AI-assisted development using Claude Code

---

## Table of Contents

1. [What Was Built Today](#1-what-was-built-today)
2. [Architecture](#2-architecture)
3. [Folder Structure](#3-folder-structure)
4. [Notification Entity Design](#4-notification-entity-design)
5. [Status Lifecycle](#5-status-lifecycle)
6. [API Reference](#6-api-reference)
7. [Validation Rules](#7-validation-rules)
8. [Database Design](#8-database-design)
9. [Exception Handling](#9-exception-handling)
10. [How I Used Claude — Step-by-Step](#10-how-i-used-claude--step-by-step)
11. [How to Run](#11-how-to-run)
12. [Test with Postman](#12-test-with-postman)
13. [What's Next](#13-whats-next)

---

## 1. What Was Built Today

| # | Task | Status |
|---|------|--------|
| 1 | Read DAY1–DAY4 progress to identify next service | ✅ Done |
| 2 | Audited notification-service scaffold (only main class existed) | ✅ Done |
| 3 | Updated `pom.xml` — Spring Boot 4.0.6, Spring Cloud 2025.1.0, JPA, MySQL, Eureka, Lombok | ✅ Done |
| 4 | Configured `application.properties` — port 8085, notification_db, HikariCP, Eureka | ✅ Done |
| 5 | Designed `Notification` entity with type, channel, status, and priority enums | ✅ Done |
| 6 | Created `NotificationDto` (split into `Request` / `Response`) | ✅ Done |
| 7 | Implemented `NotificationRepository` with soft-delete and farmer-scoped queries | ✅ Done |
| 8 | Implemented `NotificationService` interface | ✅ Done |
| 9 | Implemented `NotificationServiceImpl` with duplicate check, null-safe update, status lifecycle | ✅ Done |
| 10 | Built REST API — CRUD + `PATCH /{id}/sent` + `PATCH /{id}/read` endpoints | ✅ Done |
| 11 | Added `GlobalExceptionHandler` — 400, 404, 409, 500 | ✅ Done |
| 12 | Connected MySQL database (`notification_db`) | ✅ Done |
| 13 | Updated `CLAUDE.md` — notification-service status: Planned → Done | ✅ Done |

---

## 2. Architecture

### Where notification-service fits

```
Client (Postman / Mobile App)
          │
          ▼
   [ api-gateway ]          ← port 8080 (next)
          │
          ▼
 [ service-registry ]       ← port 8761 (Eureka)
          │
    ┌─────┴────────────────────────────────────────────────────┐
    │               │               │              │           │
[ farmer-service ] [ crop-service ] [ weather-service ] [ ai-advisor-service ] [ notification-service ] ← NEW
   port 8081         port 8082        port 8083          port 8084                port 8085
   farmer_db         crop_db          weather_db         (no DB)                  notification_db
```

### What notification-service does

`notification-service` is the **messaging layer** of the system. It stores and tracks all notifications sent to farmers.

Future integrations:
- `ai-advisor-service` → create a `CROP_ADVISORY` notification when AI advice is generated
- `weather-service` → trigger a `WEATHER_ALERT` when a dangerous forecast is recorded
- Mobile app → poll `GET /farmer/{farmerId}` to display in-app notifications

### Layered Architecture (inside notification-service)

```
HTTP Request
    │
    ▼
[ NotificationController ]     ← validates request, calls service
    │
    ▼
[ NotificationServiceImpl ]    ← business logic, duplicate check, status lifecycle
    │
    ▼
[ NotificationRepository ]     ← Spring Data JPA, soft-delete queries
    │
    ▼
[ MySQL — notification_db ]
```

---

## 3. Folder Structure

```
notification-service/
├── pom.xml
└── src/
    └── main/
        ├── java/com/smartagriculture/notificationservice/
        │   ├── NotificationServiceApplication.java
        │   ├── controller/
        │   │   └── NotificationController.java
        │   ├── service/
        │   │   ├── NotificationService.java            ← interface
        │   │   └── NotificationServiceImpl.java        ← implementation
        │   ├── repository/
        │   │   └── NotificationRepository.java
        │   ├── entity/
        │   │   └── Notification.java                   ← JPA entity with 4 enums
        │   ├── dto/
        │   │   └── NotificationDto.java                ← nested Request + Response
        │   └── exception/
        │       ├── ResourceNotFoundException.java
        │       ├── DuplicateResourceException.java
        │       └── GlobalExceptionHandler.java
        └── resources/
            └── application.properties
```

---

## 4. Notification Entity Design

### Why these fields?

The `Notification` entity models every message the system sends to a farmer, with enough metadata to support delivery tracking and multi-channel dispatch.

#### Identity Fields

| Field | Type | Why |
|---|---|---|
| `id` | `String` (UUID) | Safe for distributed systems |
| `farmerId` | `String` | Cross-service reference to farmer-service — stored as plain String, no FK |
| `title` | `String` (200 chars) | Short subject line shown in mobile push / SMS header |
| `message` | `String` (2000 chars) | Full notification body |

#### Classification Fields

| Field | Enum Values | Why |
|---|---|---|
| `type` | `WEATHER_ALERT, CROP_ADVISORY, PEST_WARNING, IRRIGATION_REMINDER, SYSTEM, OTHER` | Allows the mobile app to render the correct icon and filter by category |
| `channel` | `EMAIL, SMS, PUSH, IN_APP` | Tracks which delivery method was used — useful for analytics and retry logic |
| `status` | `PENDING, SENT, FAILED, READ` | Full delivery lifecycle — see section 5 |
| `priority` | `LOW, MEDIUM, HIGH, CRITICAL` | Determines dispatch urgency; defaults to `MEDIUM` if not provided |

#### Lifecycle Fields

| Field | Why |
|---|---|
| `deleted` | Soft delete — notification history is never physically removed |
| `createdAt` / `updatedAt` | Audit trail |
| `version` | Optimistic locking — prevents race conditions on concurrent status updates |

### Duplicate Rule

A notification is a duplicate when a `PENDING` notification with the same `farmerId + title + type + channel` already exists.

- Same farmer + same weather alert not yet sent = duplicate
- Same farmer + same alert already `SENT` = allowed (re-notification is legitimate)
- Different channel for same alert = allowed (e.g. EMAIL and SMS can coexist)

---

## 5. Status Lifecycle

```
  [create]
     │
     ▼
  PENDING  ──── PATCH /{id}/sent ────▶  SENT  ──── PATCH /{id}/read ────▶  READ
     │
     └──── (delivery failure) ────────▶  FAILED
```

| Transition | Endpoint | Guard |
|---|---|---|
| `PENDING → SENT` | `PATCH /api/v1/notifications/{id}/sent` | Only allowed from `PENDING` |
| `SENT → READ` | `PATCH /api/v1/notifications/{id}/read` | Only allowed from `SENT` |

Attempting an invalid transition returns `500 Internal Server Error` with a descriptive message.

---

## 6. API Reference

**Base URL:** `http://localhost:8085/api/v1`

---

### POST `/api/v1/notifications`
Create a new notification.

**Request Body:**
```json
{
  "farmerId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Heavy rain forecast tomorrow",
  "message": "A heavy rainfall of 80mm is predicted in Rajshahi on 2026-06-14. Consider harvesting early to avoid crop damage.",
  "type": "WEATHER_ALERT",
  "channel": "PUSH",
  "priority": "HIGH"
}
```

**Response `201 Created`:**
```json
{
  "id": "7fa85f64-5717-4562-b3fc-2c963f66afa6",
  "farmerId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Heavy rain forecast tomorrow",
  "message": "A heavy rainfall of 80mm is predicted in Rajshahi on 2026-06-14...",
  "type": "WEATHER_ALERT",
  "channel": "PUSH",
  "status": "PENDING",
  "priority": "HIGH",
  "createdAt": "2026-06-13T10:00:00",
  "updatedAt": "2026-06-13T10:00:00"
}
```

> **Duplicate rule:** A PENDING notification with the same `farmerId + title + type + channel` returns `409 Conflict`.

---

### GET `/api/v1/notifications`
Get all notifications (paginated).

**Query Parameters:**

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | Page number (0-indexed) |
| `size` | `20` | Records per page (max 100) |
| `sortBy` | `createdAt` | Field to sort by |
| `sortDir` | `desc` | `asc` or `desc` |

**Example:** `GET /api/v1/notifications?page=0&size=10&sortBy=createdAt&sortDir=desc`

---

### GET `/api/v1/notifications/{id}`
Get a single notification by UUID.

**Response `404 Not Found`:**
```json
{
  "timestamp": "2026-06-13T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Notification not found with id: abc-123"
}
```

---

### GET `/api/v1/notifications/farmer/{farmerId}`
Get all notifications for a specific farmer (paginated). Used by the mobile app to populate the notification inbox.

**Example:** `GET /api/v1/notifications/farmer/{farmerId}?page=0&size=20&sortBy=createdAt&sortDir=desc`

---

### PUT `/api/v1/notifications/{id}`
Update an existing notification. Only editable while status is still `PENDING`.

**Request Body:** Same as POST (`farmerId` and `title` always required; other fields null-safe)

**Response `200 OK`:** Returns updated `NotificationDto.Response`

---

### DELETE `/api/v1/notifications/{id}`
Soft-delete a notification (sets `deleted=true`).

**Response `200 OK`:**
```json
"Notification deleted successfully"
```

---

### PATCH `/api/v1/notifications/{id}/sent`
Mark a `PENDING` notification as `SENT` (delivery confirmed).

**Response `200 OK`:** Returns updated `NotificationDto.Response` with `"status": "SENT"`

---

### PATCH `/api/v1/notifications/{id}/read`
Mark a `SENT` notification as `READ` (farmer has seen it).

**Response `200 OK`:** Returns updated `NotificationDto.Response` with `"status": "READ"`

---

## 7. Validation Rules

| Field | Rule |
|---|---|
| `farmerId` | Required |
| `title` | Required. Max 200 characters |
| `message` | Required. Max 2000 characters |
| `type` | Required. Must be a valid `NotificationType` enum value |
| `channel` | Required. Must be a valid `NotificationChannel` enum value |
| `priority` | Optional. Defaults to `MEDIUM` if not provided |

**Validation error response `400 Bad Request`:**
```json
{
  "timestamp": "2026-06-13T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "farmerId": "Farmer ID is required",
    "type": "Notification type is required"
  }
}
```

**Duplicate error response `409 Conflict`:**
```json
{
  "timestamp": "2026-06-13T10:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "A PENDING notification with the same title, type, and channel already exists for this farmer"
}
```

---

## 8. Database Design

**Database:** `notification_db`
**Table:** `notifications`

| Column | Type | Constraint | Notes |
|---|---|---|---|
| `id` | `VARCHAR(36)` | PK | UUID auto-generated |
| `farmer_id` | `VARCHAR(255)` | NOT NULL | Cross-service ref to farmer-service |
| `title` | `VARCHAR(200)` | NOT NULL | Notification subject |
| `message` | `VARCHAR(2000)` | — | Full body text |
| `type` | `VARCHAR(30)` | NOT NULL | ENUM: WEATHER_ALERT, CROP_ADVISORY... |
| `channel` | `VARCHAR(20)` | NOT NULL | ENUM: EMAIL, SMS, PUSH, IN_APP |
| `status` | `VARCHAR(20)` | NOT NULL | Default: PENDING |
| `priority` | `VARCHAR(20)` | NOT NULL | Default: MEDIUM |
| `deleted` | `BOOLEAN` | NOT NULL | Default: false |
| `created_at` | `DATETIME` | — | Auto-set on insert |
| `updated_at` | `DATETIME` | — | Auto-set on update |
| `version` | `BIGINT` | — | Optimistic locking |

**Indexes:**
```sql
INDEX idx_notif_farmer  (farmer_id)
INDEX idx_notif_type    (type)
INDEX idx_notif_status  (status)
INDEX idx_notif_channel (channel)
INDEX idx_notif_deleted (deleted)
```

---

## 9. Exception Handling

All exceptions are handled centrally in `GlobalExceptionHandler.java`.

| Exception | HTTP Status | Trigger |
|---|---|---|
| `DuplicateResourceException` | `409 Conflict` | Same PENDING notification already exists for the farmer |
| `ResourceNotFoundException` | `404 Not Found` | ID does not exist or is soft-deleted |
| `MethodArgumentNotValidException` | `400 Bad Request` | Validation annotation fails |
| `Exception` (generic) | `500 Internal Server Error` | Unexpected runtime error (incl. invalid status transitions) |

---

## 10. How I Used Claude — Step-by-Step

**Step 1 — Orient Claude with progress history**

> *"Read my CLAUDE.md and DAY1–DAY4 progress files. Tell me what is not done."*

Claude read all four progress files and identified that `notification-service` was next (DAY4 ended with it marked as "Next") and `api-gateway` was still planned.

---

**Step 2 — Audit existing scaffold**

Claude read all existing notification-service files and identified that only the bare `NotificationServiceApplication.java` existed — everything needed to be built.

---

**Step 3 — Design the entity**

Key design decisions:
- 4 enums inside the entity class (`NotificationType`, `NotificationChannel`, `NotificationStatus`, `NotificationPriority`) to keep related constants co-located
- Status lifecycle enforced at the service layer, not the DB layer
- `farmerId` stored as a plain `String` — no FK to farmer-service (microservices stay loosely coupled)
- Duplicate check scoped to `PENDING` only — re-sending an already-sent notification is a valid use case

---

**Step 4 — Implement all layers**

Claude implemented in dependency order:
1. `pom.xml` → `application.properties`
2. `Notification.java` entity with all 4 enums
3. `NotificationDto.java` (Request + Response)
4. `NotificationRepository.java`
5. `NotificationService.java` interface
6. `NotificationServiceImpl.java` — CRUD + `markAsSent()` + `markAsRead()`
7. `NotificationController.java` — 8 endpoints
8. Exception classes + `GlobalExceptionHandler`

---

**What worked well with Claude:**
- Correctly modelled the status lifecycle (PENDING → SENT → READ) with guard checks
- Scoped the duplicate check to PENDING only — not all historical notifications
- Farmer-scoped `GET /farmer/{farmerId}` endpoint added proactively for mobile app use case
- Applied all CLAUDE.md architecture rules consistently without re-explaining them

---

## 11. How to Run

### Prerequisites
- Java 21
- Maven 3.9+
- MySQL 8.x running locally
- `service-registry` running on port `8761`

### Step 1 — Create the database
```sql
CREATE DATABASE notification_db;
```

### Step 2 — Ensure service-registry is running
```bash
cd service-registry
mvn spring-boot:run
```
Eureka dashboard: http://localhost:8761

### Step 3 — Start notification-service
```bash
cd notification-service
mvn spring-boot:run
```
Service starts at: http://localhost:8085

### Step 4 — Verify Eureka registration
Open http://localhost:8761 — you should see `NOTIFICATION-SERVICE` listed alongside the other services.

---

## 12. Test with Postman

### Create a Weather Alert Notification
```
POST http://localhost:8085/api/v1/notifications
Content-Type: application/json

{
  "farmerId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Heavy rain forecast tomorrow",
  "message": "A heavy rainfall of 80mm is predicted in Rajshahi on 2026-06-14. Consider harvesting early.",
  "type": "WEATHER_ALERT",
  "channel": "PUSH",
  "priority": "HIGH"
}
```

### Create a Crop Advisory Notification
```
POST http://localhost:8085/api/v1/notifications
Content-Type: application/json

{
  "farmerId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Best time to plant rice",
  "message": "Based on current soil moisture and temperature, this week is optimal for planting Kharif rice.",
  "type": "CROP_ADVISORY",
  "channel": "IN_APP",
  "priority": "MEDIUM"
}
```

### Test Duplicate Protection
```
POST http://localhost:8085/api/v1/notifications
(same body as the first POST above — status is still PENDING)

→ Expected: 409 Conflict
→ "A PENDING notification with the same title, type, and channel already exists for this farmer"
```

### Get All Notifications (paginated)
```
GET http://localhost:8085/api/v1/notifications?page=0&size=10&sortBy=createdAt&sortDir=desc
```

### Get Notifications by Farmer
```
GET http://localhost:8085/api/v1/notifications/farmer/{farmerId}?page=0&size=20
```

### Get Notification by ID
```
GET http://localhost:8085/api/v1/notifications/{id}
```

### Mark as Sent (PENDING → SENT)
```
PATCH http://localhost:8085/api/v1/notifications/{id}/sent

→ Expected: 200 OK — status changes to SENT
```

### Mark as Read (SENT → READ)
```
PATCH http://localhost:8085/api/v1/notifications/{id}/read

→ Expected: 200 OK — status changes to READ
```

### Soft Delete
```
DELETE http://localhost:8085/api/v1/notifications/{id}

→ Expected: 200 OK — "Notification deleted successfully"
→ Record stays in DB with deleted=true
```

---

## 13. What's Next

| Service | Status |
|---|---|
| `service-registry` | ✅ Complete |
| `farmer-service` | ✅ Complete |
| `crop-service` | ✅ Complete |
| `weather-service` | ✅ Complete |
| `ai-advisor-service` | ✅ Complete |
| `notification-service` | ✅ Complete |
| `api-gateway` | 🔲 Next |

---

*Built with Java Spring Boot · AI-assisted with Claude Code*