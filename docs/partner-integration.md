# Partner Integration Guide

This project now exposes a **versioned partner-facing API** under:

```text
/api/v1/*
```

Legacy, non-versioned routes under `/api/*` remain available for backward compatibility, but new platform-partner integrations should target `/api/v1/*`.

## Authentication

Current reference authentication uses HTTP Basic Auth.

Seeded demo users:

- `admin / Admin@123`
- `accountant / Accountant@123`
- `approver / Approver@123`

For production partner onboarding, replace this with API keys, OAuth2, or OpenID Connect.

## OpenAPI and Swagger

The backend publishes interactive documentation at:

- Swagger UI: `/swagger-ui.html`
- All API docs: `/v3/api-docs`
- Partner-only versioned docs: `/v3/api-docs/partner-v1`

## Versioning strategy

- `/api/v1/*` = preferred partner contract
- `/api/*` = legacy compatibility layer

This lets partner platforms onboard against a stable versioned path while existing internal consumers can keep operating during migration.

## Core partner workflows

### 1. Resolve current authenticated user

```http
GET /api/v1/security/me
```

### 2. Load organization dashboard

```http
GET /api/v1/dashboard/summary?organizationId=1
```

### 3. Manage employees and W-4 profiles

```http
GET  /api/v1/employees?organizationId=1
POST /api/v1/employees
PUT  /api/v1/employees/{employeeId}/w4
```

### 4. Process and approve payroll

```http
GET  /api/v1/schedules?organizationId=1
POST /api/v1/schedules/{scheduleId}/process
GET  /api/v1/payroll-runs?organizationId=1
POST /api/v1/payroll-runs/{runId}/approve
POST /api/v1/payroll/calculate
```

### 5. Access tax rules and filing records

```http
GET  /api/v1/tax/years
GET  /api/v1/tax/filings?organizationId=1&taxYear=2026
POST /api/v1/tax/filings/generate
```

## Embedding the UI

The React application consumes the versioned partner routes, and the reusable components are exported from:

```ts
frontend/src/components/index.ts
```

Platform partners can:

- embed the full app shell,
- reuse selected widgets,
- or integrate directly against the OpenAPI contract.

## Recommended next partner hardening steps

- move from Basic Auth to OAuth2/API keys,
- add request idempotency for mutation endpoints,
- add outbound webhooks for payroll/filing events,
- enforce stronger tenant isolation than request-parameter scoping,
- publish SDKs generated from the OpenAPI spec.
