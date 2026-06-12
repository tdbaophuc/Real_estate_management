# API Testing Guide

Use this guide with Swagger UI or an HTTP client when validating the Real Estate Management API.

## Local URLs

- API base URL: `http://localhost:8081`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- Swagger UI: `http://localhost:8081/swagger-ui.html`

The API is versioned under `/api/v1`.

## Authentication Flow

Most endpoints require a JWT access token. Public endpoints are limited to authentication and public listing search.

1. Register a user:

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "agent@example.com",
  "password": "StrongPass123!",
  "fullName": "Demo Agent",
  "phone": "+84901234567"
}
```

2. Log in:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "agent@example.com",
  "password": "StrongPass123!"
}
```

3. Copy `data.accessToken` from the login response.

4. In Swagger UI, click **Authorize** and enter:

```text
Bearer <accessToken>
```

5. Use the refresh token endpoint when the access token expires:

```http
POST /api/v1/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "<refreshToken>"
}
```

## Standard Success Response

All successful responses use `ApiResponse`.

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request processed successfully",
  "data": {},
  "timestamp": "2026-06-12T00:00:00Z"
}
```

List endpoints usually return `PageResponse` inside `data`.

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Request processed successfully",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  },
  "timestamp": "2026-06-12T00:00:00Z"
}
```

## Standard Error Response

All handled errors use `ApiErrorResponse`.

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "errors": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ],
  "path": "/api/v1/auth/register",
  "timestamp": "2026-06-12T00:00:00Z"
}
```

Common error codes:

- `VALIDATION_ERROR`: Bean validation failed for request body, path variables, or query parameters.
- `INVALID_REQUEST`: Request body is missing, malformed, or has an invalid parameter type.
- `RESOURCE_NOT_FOUND`: Requested entity does not exist.
- `DUPLICATE_RESOURCE`: Request would create a duplicate entity.
- `BUSINESS_RULE_VIOLATION`: Request violates a workflow or domain rule.
- `UNAUTHORIZED`: Authentication is missing, expired, or invalid.
- `FORBIDDEN`: Authenticated user lacks required permissions.
- `RATE_LIMIT_EXCEEDED`: The configured rate limit window was exceeded.
- `FILE_UPLOAD_ERROR`: Uploaded file is invalid or too large.
- `AI_PROVIDER_ERROR`: AI provider call failed.
- `INTERNAL_SERVER_ERROR`: Unexpected server error.

## Endpoint Groups

Swagger UI groups endpoints by domain:

- `Authentication`: register, login, refresh token, logout, current user.
- `User Management`: admin user search, lookup, status, and role assignment.
- `Properties`: property inventory, images, cover image, and status.
- `Listings`: private listing workflow, public listing search, and favorites.
- `Customers`: customer profiles, notes, requirements, and timeline.
- `Leads`: lead intake, assignment, status, notes, activities, and follow-up tasks.
- `Appointments`: viewing workflow, reschedule, cancellation, completion, and feedback.
- `Contracts`: contract drafting, upload, review, approval, signing, and cancellation.
- `Transactions`: deposits, schedules, payments, invoices, and receipts.
- `Commissions`: commission rules, agent commissions, and payout status.
- `Notifications`: notification inbox, unread count, and read state.
- `Dashboard` and `Reports`: role summaries and business reporting.
- `AI`: chat, recommendations, scoring, summaries, descriptions, and image analysis.
- `Files`: multipart uploads.
- `Audit Logs`: admin audit search and detail lookup.

## Quick Smoke Test

After the application starts, verify documentation and authentication wiring:

```powershell
Invoke-RestMethod http://localhost:8081/v3/api-docs | Select-Object -ExpandProperty info
```

Then test a secured endpoint without a token. It should return an authentication error:

```powershell
Invoke-WebRequest http://localhost:8081/api/v1/auth/me
```

After login, retry with:

```powershell
$headers = @{ Authorization = "Bearer <accessToken>" }
Invoke-RestMethod http://localhost:8081/api/v1/auth/me -Headers $headers
```
