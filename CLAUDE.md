# Smart Agriculture Advisor — Project Rules

## Stack
- Java 21 · Spring Boot 4.0.6 · Spring Cloud 2025.1.0
- MySQL 8.x · Netflix Eureka · Lombok · Jakarta Validation

## Services & Ports
| Service            | Port  | DB           | Status   |
|--------------------|-------|--------------|----------|
| service-registry   | 8761  | —            | Done     |
| farmer-service     | 8081  | farmer_db    | Done     |
| crop-service       | 8082  | crop_db      | Done     |
| weather-service    | 8083  | weather_db   | Done     |
| ai-advisor-service | 8084  | —            | Done     |
| notification-service | 8085 | notification_db | Done  |
| api-gateway        | 8080  | —            | Done     |

## Architecture Rules (apply to every service)

### Entity Design
- UUID primary key: `@GeneratedValue(strategy = GenerationType.UUID)`, stored as `String`
- Soft delete: `deleted` boolean field, never physically remove rows
- Optimistic locking: `@Version Long version`
- Audit: `@CreationTimestamp createdAt`, `@UpdateTimestamp updatedAt`
- Enums stored as `@Enumerated(EnumType.STRING)`
- No `@NotBlank` or other validation annotations on entity fields — validation belongs in DTOs only

### DTO Design
- Always split into `Request` (create/update input) and `Response` (API output)
- `Response` never exposes `deleted`, `version`
- All validation annotations (`@NotBlank`, `@Size`, `@Pattern`, etc.) go on `Request` only

### Service Layer
- Duplicate check before insert and update — throw `DuplicateResourceException` (409)
- `updateX` method is null-safe: only overwrite a field when the request value is non-null
  - Exception: `name` is always required (`@NotBlank`) so it is always set
- Soft delete sets `deleted = true` and `status = INACTIVE`

### Exception Handling
- `ResourceNotFoundException` → 404
- `DuplicateResourceException` → 409
- `MethodArgumentNotValidException` → 400 with `fieldErrors` map
- `Exception` (catch-all) → 500
- All handled in `GlobalExceptionHandler` with `@RestControllerAdvice`

### API Design
- Versioned base path: `/api/v1/<resource>`
- Paginated `GET` with `page`, `size` (max 100), `sortBy`, `sortDir` params
- `POST` returns `201 Created`
- `DELETE` returns `200 OK` with plain string message (soft delete)

### application.properties Pattern
- Port: next available (8081, 8082, 8083 ...)
- HikariCP: `maximum-pool-size=20`, `minimum-idle=5`
- `spring.jpa.hibernate.ddl-auto=update`
- Eureka: register + fetch, prefer-ip-address

### pom.xml Pattern
- Parent: `spring-boot-starter-parent` 4.0.6
- Spring Cloud BOM: `2025.1.0`
- Always include: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`,
  `spring-boot-starter-validation`, `mysql-connector-j` (runtime),
  `spring-cloud-starter-netflix-eureka-client`, `lombok` (optional)