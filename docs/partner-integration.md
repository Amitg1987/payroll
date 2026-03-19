# Partner Integration Guide

This project exposes a **versioned partner-facing API** under:

```text
/api/v1/*
```

Legacy `/api/*` routes remain available for compatibility, but new partner/platform integrations should target the versioned surface.

## 1. Authentication options

### Interactive developer/integrator login

Use seeded developer credentials:

- `integrator / Integrator@123`

This role maps to `DEVELOPER_PLATFORM_INTEGRATOR`.

### API key partner auth

Programmatic integrations can use:

```http
X-API-Key: pk_live_demo_embedded_payroll_partner_2026
```

This authenticates as the seeded partner API client for the default tenant.

## 2. OpenAPI / Swagger

- Swagger UI: `/swagger-ui.html`
- full docs: `/v3/api-docs`
- partner-v1 docs: `/v3/api-docs/partner-v1`

## 3. Key partner API workflows

### Resolve current actor / tenant context

```http
GET /api/v1/security/me
```

### Load company + jurisdiction registrations

```http
GET /api/v1/organizations/current
GET /api/v1/dashboard/summary?organizationId=1
```

### Programmatic employee onboarding

```http
GET  /api/v1/employees?organizationId=1
POST /api/v1/employees
PUT  /api/v1/employees/{employeeId}/w4
```

### Payroll processing and reporting

```http
GET  /api/v1/schedules?organizationId=1
POST /api/v1/schedules/{scheduleId}/process
GET  /api/v1/payroll-runs?organizationId=1
POST /api/v1/payroll/calculate
GET  /api/v1/reports/payroll-summary?organizationId=1&taxYear=2026
```

Both payroll processing and ad hoc calculations accept optional
`workLocationAllocations` so multi-state wages can be split across work states
before state/local withholding is calculated.

Multi-state logic currently applies these rules:

- work-state withholding on wages earned in that state
- resident-state withholding on all income for taxing resident states
- reciprocity checked first
- resident-state credit offsets when reciprocity does not apply
- persisted allocation tracking per work location

### Tax rules, filings, and workflows

```http
GET  /api/v1/tax/years
GET  /api/v1/tax/jurisdictions
GET  /api/v1/tax/jurisdiction-profiles?taxYear=2026&taxType=STATE_WITHHOLDING
GET  /api/v1/tax/filings?organizationId=1&taxYear=2026
POST /api/v1/tax/filings/generate
GET  /api/v1/tax/workflows/filings?organizationId=1
POST /api/v1/tax/workflows/filings
```

### Partner API client and webhook management

```http
GET  /api/v1/integration/api-clients?organizationId=1
POST /api/v1/integration/api-clients
GET  /api/v1/integration/webhooks?organizationId=1
POST /api/v1/integration/webhooks
GET  /api/v1/integration/webhook-deliveries?organizationId=1
POST /api/v1/integration/webhook-deliveries/dispatch
```

## 4. Idempotency

Mutation endpoints support caller-supplied idempotency keys:

```http
Idempotency-Key: create-employee-2026-0001
```

This is especially useful for:

- employee creation
- schedule creation
- payroll processing
- filing generation
- filing workflow start
- API client issuance
- webhook creation

Multi-state payroll requests should always use idempotency keys when they are
submitted from partner platforms, because allocation payloads often originate
from external time or workforce systems.

## 5. Webhooks

Webhook endpoints are tenant-scoped and may also be associated with a partner API client.

The platform signs outbound payloads with HMAC SHA-256 and sends:

- `X-Webhook-Event`
- `X-Webhook-Event-Key`
- `X-Webhook-Signature`

Current event examples include payroll run and tax filing workflow activity.

Because payroll writes are transactional, webhook events are only enqueued after
the associated payroll or filing records are successfully persisted.

## 6. Temporal-backed filings

Tax filing workflows are modeled with Temporal workflow/activity definitions.

When Temporal is enabled, the workflow start endpoint launches a Temporal workflow.
When it is disabled for local development, the same endpoint falls back to local synchronous generation while preserving the workflow request record.

## 7. Embedding the UI

The React employer dashboard consumes the versioned partner API and exports reusable components from:

```ts
frontend/src/components/index.ts
```

Partner platforms can:

- embed the dashboard wholesale,
- reuse selected widgets,
- or consume the API contract directly.

## 8. Recommended production hardening

- rotate API keys and webhook secrets through a secret manager
- add tenant-aware gateway rate limits
- publish SDKs generated from the OpenAPI spec
- add stronger tenant partitioning at the persistence layer
- integrate certified filing/remittance providers
- run Temporal against a managed or production-grade cluster
