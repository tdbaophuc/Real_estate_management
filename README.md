# Real Estate Management Backend

## Overview

Backend API for a real estate management platform used by admins, managers,
agents, customers, and owners. The project has moved from the original training
building CRUD app into a modular monolith that covers property operations, CRM,
listing publication, appointments, contracts, transactions, commissions,
notifications, dashboards, audit logs, and AI-assisted workflows.

The active development branch is `breakthrough`. The roadmap and day-by-day
workflow live in:

- `docs/REAL_ESTATE_MANAGEMENT_PLAN.md`
- `docs/DAILY_DEVELOPMENT_WORKFLOW.md`
- `docs/DAILY_PROMPTS.md`

## Features

Current backend modules include:

- Authentication and authorization: register, login, refresh token, logout,
  current user, JWT security, role/permission model, and admin user management.
- Property management: property CRUD, address data, types, amenities, legal
  documents, status changes, search filters, and image management.
- File storage: local upload service, metadata persistence, content type and
  size validation, and property image integration.
- Listing workflows: create and edit listings, submit for review, approve,
  reject, publish, unpublish, public listing search, views, and favorites.
- Customer CRM: customer profiles, requirements, notes, search, detail, and
  timeline data.
- Lead pipeline: lead creation, assignment, pipeline status updates, notes,
  activities, and follow-up tasks.
- Appointment management: booking, conflict checks, confirmation, cancellation,
  rescheduling, completion, and viewing feedback.
- Notifications and reminders: in-app notifications, unread counts, mark-read
  APIs, email delivery abstraction, and reminder jobs.
- Contracts: contract creation, update, document upload, review, approval,
  signed status, and cancellation.
- Transactions and payments: transaction lifecycle, deposits, payment schedules,
  offline payment records, invoices, receipts, and idempotency keys.
- Commissions: commission rules, commission calculation, personal commission
  list, manager/admin list, and mark-paid workflow.
- Dashboards and reports: admin, manager, and agent dashboards plus revenue,
  lead, transaction, and commission reports.
- Audit log: admin search/detail APIs for important business events.
- AI features: provider abstraction, request logging, listing description,
  property recommendation, lead scoring, chatbot sessions, customer summaries,
  and property image analysis skeletons with fallback behavior.

Legacy `/api/buildings/**` code still exists from the original course project.
New clients should use `/api/v1/**` APIs.

## Tech Stack

- Java 21
- Spring Boot 3.2.2
- Spring Web
- Spring Security
- Spring Data JPA and Hibernate
- Bean Validation
- PostgreSQL 16 for local/dev/uat/prod
- Flyway database migrations
- H2 in PostgreSQL compatibility mode for fast tests
- JWT with `jjwt`
- ModelMapper
- Springdoc OpenAPI / Swagger UI
- JUnit 5 and Spring Boot Test
- Testcontainers for PostgreSQL integration coverage
- Docker Compose for PostgreSQL, Redis, MinIO, and MailHog

## Local Setup

### Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop or Docker Engine with Compose v2

### 1. Create the local environment file

PowerShell:

```powershell
Copy-Item .env.example .env
```

macOS/Linux:

```bash
cp .env.example .env
```

The `.env` file is ignored by Git. It contains local credentials and ports for
PostgreSQL, Redis, MinIO, MailHog, and AI placeholders. If you change
`DB_NAME` or `DB_PORT`, also update `DB_URL`.

### 2. Start local infrastructure

```bash
docker compose up -d
docker compose ps
```

Local services:

| Service | Address | Purpose |
|---|---|---|
| PostgreSQL | `localhost:5432` | Application database |
| Redis | `localhost:6379` | Cache/session-ready infrastructure |
| MinIO API | `http://localhost:9000` | S3-compatible storage-ready infrastructure |
| MinIO console | `http://localhost:9001` | Object storage administration |
| MailHog SMTP | `localhost:1025` | Local SMTP target |
| MailHog UI | `http://localhost:8025` | Inspect local email |

The `minio-init` service creates the bucket configured by `MINIO_BUCKET`.
Uploads currently use local storage by default; MinIO is provisioned for later
storage adapters.

### 3. Load environment variables

PowerShell:

```powershell
Get-Content .env |
  Where-Object { $_ -match '^[^#][^=]*=' } |
  ForEach-Object {
    $name, $value = $_ -split '=', 2
    Set-Item -Path "Env:$name" -Value $value
  }
```

macOS/Linux:

```bash
set -a
source .env
set +a
```

### 4. Run the application

```bash
mvn spring-boot:run
```

Useful local URLs:

- API base URL: `http://localhost:8081/api/v1`
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

The `dev` profile is active by default. Flyway runs all pending migrations when
the application starts.

### 5. Optional local admin seed

Set these variables before startup to create a development administrator:

```text
DEV_ADMIN_ENABLED=true
DEV_ADMIN_EMAIL=admin@realestate.local
DEV_ADMIN_PASSWORD=<strong local password>
DEV_ADMIN_FULL_NAME=Development Administrator
```

### 6. Stop local infrastructure

```bash
docker compose down
```

Remove local Docker volumes as well:

```bash
docker compose down -v
```

## Configuration Profiles

The shared configuration is in `src/main/resources/application.yml`.

| Profile | File | Purpose |
|---|---|---|
| `dev` | `application-dev.yml` | Local development; default active profile |
| `test` | `application-test.yml` | Automated tests with H2 and test defaults |
| `uat` | `application-uat.yml` | Staging/demo style configuration |
| `prod` | `application-prod.yml` | Production style configuration |

Important environment variables:

| Variable | Description |
|---|---|
| `SERVER_PORT` | HTTP port, default `8081` |
| `DB_URL` | JDBC URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Base64 JWT signing secret |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access token lifetime, default `PT30M` |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Refresh token lifetime, default `P30D` |
| `CORS_ALLOWED_ORIGINS` / `FRONTEND_ORIGIN` | Allowed frontend origins |
| `LOCAL_STORAGE_ROOT` | Local upload directory |
| `MAX_UPLOAD_SIZE` | Maximum upload file size |
| `REMINDERS_ENABLED` | Enables reminder scheduler |
| `AI_ENABLED` | Enables configured AI provider calls |
| `AI_PROVIDER` | AI provider adapter name, default `noop` |
| `AI_API_KEY` | External provider API key |
| `AI_MODEL` | External provider model name |
| `AI_TIMEOUT` | External provider timeout |

Production disables Swagger UI by default through `SWAGGER_UI_ENABLED=false`
and disables OpenAPI JSON unless `OPENAPI_ENABLED=true`.

## Database Migrations

Flyway migrations are stored in `src/main/resources/db/migration`.

Current migration range:

```text
V001__init_auth_schema.sql
V002__complete_auth_schema_and_seed_permissions.sql
V003__create_property_schema_and_seed_master_data.sql
V004__create_file_resources.sql
V005__link_property_images_to_file_resources.sql
V006__create_listing_schema.sql
V007__create_customer_schema.sql
V008__create_lead_pipeline_schema.sql
V009__create_appointment_schema.sql
V010__create_notification_schema.sql
V011__add_email_reminder_support.sql
V012__create_contract_schema.sql
V013__create_transaction_payment_schema.sql
V014__create_audit_log_schema.sql
V015__create_ai_request_log_schema.sql
V016__create_ai_recommendation_schema.sql
V017__create_ai_lead_score_schema.sql
V018__create_ai_chat_schema.sql
V019__create_ai_image_analysis_schema.sql
V020__add_performance_indexes.sql
```

Rules for new schema changes:

- Add a new `V###__description.sql` file. Do not edit already-applied
  migrations unless the database is intentionally being reset.
- Keep JPA mappings aligned with Flyway schema.
- Add indexes for search, listing, report, and audit queries when new access
  patterns require them.
- Keep seed data deterministic and safe for repeated local setup.

## API Documentation

Runtime documentation:

- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

Static frontend reference:

- `docs/API_FRONTEND_REFERENCE.md`

API conventions:

- Main API prefix: `/api/v1`
- JSON content type: `application/json`
- Auth header: `Authorization: Bearer <accessToken>`
- List APIs use zero-based pagination with `page`, `size`, `sortBy`, and
  `direction` where supported.
- Successful responses use the common envelope:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request processed successfully",
  "data": {},
  "timestamp": "2026-06-13T00:00:00Z"
}
```

- Error responses use the common error envelope with `code`, `message`,
  optional validation `errors`, `path`, and `timestamp`.

Representative endpoint groups:

| Module | Endpoint prefix |
|---|---|
| Auth | `/api/v1/auth` |
| User management | `/api/v1/admin/users` |
| Properties | `/api/v1/properties` |
| Listings | `/api/v1/listings`, `/api/v1/search/listings` |
| Customers | `/api/v1/customers` |
| Leads | `/api/v1/leads` |
| Appointments | `/api/v1/appointments` |
| Notifications | `/api/v1/notifications` |
| Contracts | `/api/v1/contracts` |
| Transactions | `/api/v1/transactions` |
| Commissions | `/api/v1/commissions`, `/api/v1/commission-rules` |
| Dashboards | `/api/v1/dashboard` |
| Reports | `/api/v1/reports` |
| Audit logs | `/api/v1/audit-logs` |
| Files | `/api/v1/files` |
| AI | `/api/v1/ai` |

## AI Features

The AI module is designed around provider abstraction, request logging,
timeouts, rate limits, and deterministic fallbacks.

Implemented endpoint groups:

- `POST /api/v1/ai/listing-description`
- `POST /api/v1/ai/customers/{customerId}/recommendations`
- `POST /api/v1/ai/leads/{leadId}/score`
- `POST /api/v1/ai/chat/sessions`
- `POST /api/v1/ai/chat/sessions/{sessionId}/messages`
- `GET /api/v1/ai/chat/sessions/{sessionId}`
- `GET /api/v1/ai/customers/{customerId}/summary`
- `POST /api/v1/ai/property-images/analyze`

Default local behavior uses `AI_PROVIDER=noop` and `AI_ENABLED=false`, so the
application does not call external AI services without explicit configuration.
AI requests are logged and services return fallback or rule-based output where
the feature supports it.

To enable a real provider later, configure at least:

```text
AI_ENABLED=true
AI_PROVIDER=<provider-name>
AI_API_KEY=<secret>
AI_MODEL=<model-name>
AI_TIMEOUT=PT10S
```

Do not commit real provider keys, prompt secrets, customer private data, access
tokens, or generated `.env` files.

## Development Workflow

Daily work should follow `docs/DAILY_DEVELOPMENT_WORKFLOW.md`:

1. Confirm the current branch is `breakthrough`.
2. Check `git status --short` and keep unrelated local changes out of the task.
3. Read the plan and pick the current day/task from `docs/DAILY_PROMPTS.md`.
4. Implement only the task scope unless a blocking fix is required.
5. Run verification that matches the change.
6. Review `git diff --stat` and stage only task-related files.
7. Commit with a clear conventional-style message.
8. Push only to `tdbaophuc breakthrough`.

Useful commands:

```bash
git branch --show-current
git status --short
mvn test
mvn -DskipTests package
git diff --stat
git push tdbaophuc breakthrough
```

For this repository, avoid committing IDE files, `.env`, generated local
storage, Docker volumes, secrets, or unrelated profile changes.

## Verification

Run the full test suite:

```bash
mvn test
```

Build without running tests:

```bash
mvn -DskipTests package
```

Validate Docker Compose configuration after creating `.env`:

```bash
docker compose config
docker compose ps
```

## Project Structure

Main source tree:

```text
src/main/java/com/javaweb/
  ai/
  appointment/
  audit/
  auth/
  commission/
  common/
  config/
  contract/
  customer/
  dashboard/
  lead/
  listing/
  notification/
  property/
  storage/
  transaction/
```

Other important paths:

```text
src/main/resources/application.yml
src/main/resources/application-dev.yml
src/main/resources/application-test.yml
src/main/resources/application-uat.yml
src/main/resources/application-prod.yml
src/main/resources/db/migration/
docs/
compose.yaml
.env.example
```
