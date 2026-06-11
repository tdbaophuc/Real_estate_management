# Real Estate Management Backend

REST API for managing real estate listings, CRM workflows, appointments,
contracts, transactions, commissions, notifications, dashboards, reports, and
audit logs.

## Technology

- Java 21 and Spring Boot 3.2
- Spring Web, Security, Data JPA, and Validation
- JWT access and refresh tokens
- PostgreSQL 16 and Flyway
- OpenAPI/Swagger UI
- JUnit 5, Spring Boot Test, and H2 for automated tests
- Docker Compose for local infrastructure

## Local Development

### Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop or Docker Engine with Compose v2

### 1. Configure local environment

Copy the example environment file:

```powershell
Copy-Item .env.example .env
```

On macOS or Linux:

```bash
cp .env.example .env
```

The values in `.env` are local development credentials. Change them as needed;
the file is ignored by Git. If you change `DB_NAME` or `DB_PORT`, update
`DB_URL` in the same file.

### 2. Start infrastructure

```bash
docker compose up -d
docker compose ps
```

The Compose stack provides:

| Service | Local address | Purpose |
|---|---|---|
| PostgreSQL | `localhost:5432` | Application database |
| Redis | `localhost:6379` | Ready for cache/session integration |
| MinIO API | `http://localhost:9000` | Ready for S3-compatible storage integration |
| MinIO console | `http://localhost:9001` | Object storage administration |
| MailHog SMTP | `localhost:1025` | Ready for SMTP integration |
| MailHog UI | `http://localhost:8025` | Inspect local email |

The `minio-init` one-shot service creates the bucket configured by
`MINIO_BUCKET`.

The application uses PostgreSQL directly. File uploads still use
`LOCAL_STORAGE_ROOT`, development email uses the logging sender, and Redis,
MinIO, and MailHog are provisioned for later adapters.

### 3. Run the application

PowerShell:

```powershell
Get-Content .env |
  Where-Object { $_ -match '^[^#][^=]*=' } |
  ForEach-Object {
    $name, $value = $_ -split '=', 2
    Set-Item -Path "Env:$name" -Value $value
  }
mvn spring-boot:run
```

On macOS or Linux:

```bash
set -a
source .env
set +a
mvn spring-boot:run
```

Useful URLs:

- API base URL: `http://localhost:8081/api/v1`
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

The `dev` profile is active by default. Flyway applies all migrations when the
application starts.

Optional development administrator:

```text
DEV_ADMIN_ENABLED=true
DEV_ADMIN_EMAIL=admin@realestate.local
DEV_ADMIN_PASSWORD=<a strong local password>
```

### 4. Stop infrastructure

```bash
docker compose down
```

To also remove local database, Redis, and MinIO data:

```bash
docker compose down -v
```

## Configuration

Important environment variables:

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/Real_estate_management_dev` | JDBC URL override |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | Required | Database password |
| `JWT_SECRET` | Development-only fallback | Base64 JWT signing secret |
| `SERVER_PORT` | `8081` | HTTP port |
| `LOCAL_STORAGE_ROOT` | `./var/storage` | Local upload directory |
| `MAX_UPLOAD_SIZE` | `10MB` | Maximum file size |
| `REMINDERS_ENABLED` | `true` in dev | Scheduled reminders |
| `AI_ENABLED` | `false` | Enables AI provider calls when a key is configured |
| `AI_PROVIDER` | `noop` | AI provider adapter name |
| `AI_API_KEY` | Empty | Provider API key from environment |
| `AI_TIMEOUT` | `PT10S` | AI provider timeout |

Production and UAT profiles require environment-provided credentials and disable
development defaults.

Profile database defaults:

| Profile | Default JDBC URL |
|---|---|
| `dev` | `jdbc:postgresql://localhost:5432/Real_estate_management_dev` |
| `uat` | `jdbc:postgresql://localhost:5432/Real_estate_management_uat` |
| `prod` | `jdbc:postgresql://localhost:5432/Real_estate_management_pro` |

The automated `test` profile keeps an in-memory H2 database in PostgreSQL mode
for fast unit/integration tests. PostgreSQL compatibility is verified by
Testcontainers using `Real_estate_management_test` inside the container.

AI provider abstraction is present, but the default `noop` provider does not
call an external service. Without `AI_ENABLED=true` and `AI_API_KEY`, requests
are skipped and logged for later feature integration.

## Verification

Run the complete test suite:

```bash
mvn test
```

Build without running tests:

```bash
mvn -DskipTests package
```

Validate and inspect Compose after creating `.env`:

```bash
docker compose config
docker compose ps
```

## API Documentation

- Interactive documentation is available through Swagger UI while the app runs.
- Frontend integration details are maintained in
  [`docs/API_FRONTEND_REFERENCE.md`](docs/API_FRONTEND_REFERENCE.md).

## Project Structure

The backend is a modular monolith. Business modules live under
`src/main/java/com/javaweb`, including:

```text
audit/          auth/           appointment/    commission/
contract/       customer/       dashboard/      lead/
listing/        notification/   property/       storage/
transaction/    common/         config/
```

Database migrations are in `src/main/resources/db/migration`.
