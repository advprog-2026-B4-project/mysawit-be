# MySawit Backend

REST API for the MySawit palm oil estate management system, built with **Spring Boot 4.0.3** and **Java 21**. Follows a **hexagonal (ports & adapters) architecture** organised by vertical feature modules.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Database | PostgreSQL 17 |
| Cache / Session | Redis 7 |
| Migrations | Flyway |
| ORM | Spring Data JPA (Hibernate) |
| Auth | JWT + Google OAuth2 |
| File Storage | Cloudflare R2 (S3-compatible) |
| Build Tool | Gradle (Kotlin DSL) |
| Code Quality | JaCoCo, SonarQube |

---

## Project Structure

```
mysawit-be/
└── src/
    └── main/
        └── java/id/ac/ui/cs/advprog/mysawitbe/
            ├── MysawitBeApplication.java       # Entry point
            ├── common/                         # Cross-cutting concerns
            │   ├── config/                     # Spring beans, security config
            │   ├── dto/
            │   │   ├── ApiResponse.java        # Generic response wrapper
            │   │   ├── FileUploadResponse.java # File upload result DTO
            │   │   └── ValidationErrorResponse.java
            │   ├── event/                      # Spring application events
            │   ├── exception/
            │   │   └── GlobalExceptionHandler.java
            │   └── port/
            │       └── StoragePort.java        # Abstraction for file storage
            └── modules/
                ├── auth/                       # Authentication & user management
                ├── kebun/                      # Plantation (kebun) management
                ├── panen/                      # Harvest (panen) management
                ├── pengiriman/                 # Delivery management
                ├── pembayaran/                 # Payroll & wallet
                └── notification/              # In-app notifications
```

Each module follows the same **hexagonal layout**:

```
<module>/
├── application/
│   ├── dto/          # Request & Response DTOs (Java Records)
│   ├── event/        # Domain events published by this module
│   └── port/
│       ├── in/       # Use-case interfaces (driven by controllers)
│       └── out/      # Repository & service ports (drive adapters)
├── domain/           # Domain models, enums, value objects
└── infrastructure/   # Adapters: JPA entities, controllers, external clients
```

---

## Modules

### `auth`
Handles user registration, login (JWT), Google OAuth2 sign-in, and user queries (list, filter by role, current user).

**Use cases:** `AuthCommandUseCase`, `UserQueryUseCase`

### `kebun`
Manages plantation records. Admins create/edit kebun and assign a mandor.

**Use cases:** `KebunCommandUseCase`, `KebunQueryUseCase`

### `panen`
Records harvest sessions. Mandor marks harvests, attaches photos (Cloudflare R2), and admins approve or reject.

**Use cases:** `PanenCommandUseCase`, `PanenQueryUseCase`

### `pengiriman`
Tracks deliveries from estate to mill. Admins assign drivers; drivers update delivery status and upload proof photos.

**Use cases:** `PengirimanCommandUseCase`, `PengirimanQueryUseCase`

### `pembayaran`
Processes worker wages. Calculates payroll from harvest data, manages wallet top-ups, and records payment history.

**Use cases:** `PembayaranCommandUseCase`, `PembayaranQueryUseCase`, `WalletQueryUseCase`

### `notification`
Delivers in-app notifications to users. Supports read/unread state and bulk mark-all-read.

---

## Common Layer

### `ApiResponse<T>`
All endpoints return this wrapper:

```json
{ "success": true, "message": "OK", "data": { ... } }
{ "success": false, "message": "Validation failed", "error": { "field": "msg" } }
```

Factory methods: `ApiResponse.success(data)`, `ApiResponse.error(message)`, `ApiResponse.fail(errors)`.

### `GlobalExceptionHandler`
Centralises exception → HTTP status mapping:

| Exception | HTTP |
|---|---|
| `EntityNotFoundException` | 404 |
| `MethodArgumentNotValidException` | 400 + field errors |
| `IllegalArgumentException` | 400 |
| `IllegalStateException` | 409 |
| `Exception` (catch-all) | 500 |

### `StoragePort`
Interface for file operations (Cloudflare R2):
- `uploadFile(byte[], fileName, contentType) → FileUploadResponse`
- `deleteFile(fileKey)`
- `getPublicUrl(fileKey) → String`

---

## Environment Variables

Copy `.env.example` → `.env` and fill in:

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/mysawit` | JDBC connection URL |
| `DB_USERNAME` | `postgres` | DB username |
| `DB_PASSWORD` | `postgres` | DB password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | _(empty)_ | Redis auth password |
| `JWT_SECRET` | _(change me)_ | Min 256-bit signing secret |
| `JWT_EXPIRATION_MS` | `86400000` | Token TTL (ms) — default 24 h |
| `GOOGLE_CLIENT_ID` | — | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | — | Google OAuth2 client secret |
| `R2_ENDPOINT` | — | Cloudflare R2 S3 endpoint URL |
| `R2_BUCKET` | `mysawit-bucket` | R2 bucket name |
| `R2_ACCESS_KEY` | — | R2 access key |
| `R2_SECRET_KEY` | — | R2 secret key |
| `R2_PUBLIC_URL` | — | Public CDN URL for R2 objects |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated allowed origins |

---

## Running Locally

### Option 1 — Docker Compose (recommended)

Spins up PostgreSQL, Redis, and the backend together:

```bash
# from mysawit-be/
docker compose up --build
```

| Service | URL |
|---|---|
| API | http://localhost:8080 |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |
| Remote debug | 5005 (JDWP) |

### Option 2 — Gradle only (requires local Postgres + Redis)

```bash
# from mysawit-be/
./gradlew bootRun
```

The application runs on port **8080** by default.

### Running Migrations Only

Flyway migrations run automatically on startup. Migration files live in:
```
src/main/resources/db/migration/V*.sql
```

---

## Building

```bash
./gradlew build
```

The fat JAR is produced in `build/libs/mysawit-be-0.0.1-SNAPSHOT.jar`.

---

## Testing

```bash
# Run all unit tests + generate JaCoCo report
./gradlew test

# JaCoCo HTML report
build/reports/jacoco/test/html/index.html
```

Functional tests (tagged `*FunctionalTest`) are excluded from the default test task and must be run explicitly:

```bash
./gradlew test --tests "*FunctionalTest"
```

---

## Code Quality — SonarQube

```bash
./gradlew sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<token>
```

Configuration is in `sonar-project.properties`.

---

## API Conventions

- All responses are wrapped in `ApiResponse<T>`.
- Validation errors return HTTP 400 with a `Map<String, String>` of field → message.
- UUIDs are used for all entity identifiers.
- Repository port methods return `null` (not `Optional`) when an entity is not found; use-case implementations throw `EntityNotFoundException` in that case.
