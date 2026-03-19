# Embedded US Payroll and Tax Platform

Reference implementation of a **multi-tenant payroll and tax platform for the US market** that can be embedded into external products via:

- a versioned REST API
- reusable React + TypeScript UI components
- partner API keys
- webhook events

The stack includes:

- **Backend:** Java 21 + Spring Boot
- **Frontend:** React + TypeScript + Vite
- **Workflow orchestration:** Temporal-compatible tax filing workflows

> This repository is a strong implementation starter for embedded payroll and tax operations. It models US payroll concepts and integration patterns, but a production launch still requires certified tax content, payment rails, operational controls, and compliance validation.

---

## 1. What the project now supports

### Company / organization and jurisdiction support

- multi-tenant organization model
- tenant key per company
- company registration and nexus records for state/local jurisdictions
- company primary payroll jurisdiction and headquarters local jurisdiction

### Payroll engine

- W-2 employee payroll processing
- multi-tenant gross-to-net calculations
- salary and hourly worker support
- employee W-4 profile management by tax year
- scheduled payroll runs with approval workflows
- atomic transactional writes across payroll headers, items, and event/outbox artifacts

### Tax engine

- federal payroll taxes:
  - FIT
  - FICA / Social Security
  - Medicare
  - Additional Medicare
  - FUTA
- seeded support for **all 50 US states**
- data-driven support for **local jurisdictions**
- employer-side state unemployment tax support
- historical federal tax year support for 2021-2026

### Filing automation

- tax filing records for:
  - Form 941
  - Form 940
  - Form W-2
  - Form W-3
  - state withholding
  - local withholding
- automated filing workflow requests using **Temporal-compatible workflow definitions**
- local development fallback when Temporal is not enabled

### Partner integration

- versioned API surface under **`/api/v1/*`**
- legacy `/api/*` compatibility routes retained
- **API key partner auth**
- **idempotency key support** for key create/update operations
- **webhook endpoint registration and delivery tracking**
- developer / platform integrator role and dashboard features

### Employer dashboard

- payroll run monitoring
- payroll reporting summary
- company jurisdiction visibility
- partner API client management
- webhook management and delivery visibility
- filing generation and workflow tracking

---

## 2. Repository structure

```text
.
├── backend/                 Spring Boot payroll API and workflow integration
├── frontend/                React + TypeScript employer dashboard + embeddable UI
├── docs/
│   ├── schema.md            Detailed database schema
│   └── partner-integration.md
└── README.md
```

---

## 3. Backend architecture

### Core modules

- `config/`
  - security
  - CORS
  - OpenAPI / Swagger
  - Temporal configuration
- `controller/`
  - security, dashboard, employees, payroll, tax
  - organization profile and jurisdictions
  - reporting
  - partner integration (API clients, webhooks, deliveries)
- `model/`
  - organizations and organization jurisdictions
  - users and partner API clients
  - employees and W-4 profiles
  - payroll schedules, runs, and run items
  - federal tax years / brackets
  - tax jurisdictions and jurisdiction tax profiles
  - idempotency records
  - webhook endpoints and deliveries
  - tax filing records and workflow requests
- `service/`
  - payroll engine
  - tax engine
  - reporting
  - idempotency support
  - webhook dispatch
  - partner integration services
  - Temporal-compatible tax filing workflow support

### Versioned API strategy

- preferred partner contract: **`/api/v1/*`**
- legacy compatibility contract: **`/api/*`**

### OpenAPI / Swagger

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- full API docs: `http://localhost:8080/v3/api-docs`
- partner-v1 docs: `http://localhost:8080/v3/api-docs/partner-v1`

---

## 4. Major API areas

### Security and tenant context

| Endpoint | Method | Purpose |
| --- | --- | --- |
| `/api/v1/security/me` | GET | current user or API-key actor |
| `/api/v1/dashboard/summary` | GET | tenant dashboard summary |
| `/api/v1/organizations/current` | GET | current company + jurisdiction registrations |

### Employee and payroll

| Endpoint | Method | Purpose |
| --- | --- | --- |
| `/api/v1/employees` | GET/POST | list or create employees |
| `/api/v1/employees/{id}/w4` | PUT | upsert W-4 profile |
| `/api/v1/schedules` | GET/POST | list or create payroll schedules |
| `/api/v1/schedules/{id}/process` | POST | process payroll for a schedule |
| `/api/v1/payroll-runs` | GET | payroll run history |
| `/api/v1/payroll-runs/{id}/approve` | POST | approve payroll run |
| `/api/v1/payroll/calculate` | POST | gross-to-net calculation |
| `/api/v1/reports/payroll-summary` | GET | employer payroll reporting summary |

### Tax and jurisdictions

| Endpoint | Method | Purpose |
| --- | --- | --- |
| `/api/v1/tax/years` | GET | federal tax years |
| `/api/v1/tax/jurisdictions` | GET | all jurisdictions |
| `/api/v1/tax/jurisdiction-profiles` | GET | state/local tax profiles by year/type |
| `/api/v1/tax/filings` | GET | filing records |
| `/api/v1/tax/filings/generate` | POST | direct filing generation |
| `/api/v1/tax/workflows/filings` | GET/POST | Temporal-compatible filing workflows |

### Partner integration

| Endpoint | Method | Purpose |
| --- | --- | --- |
| `/api/v1/integration/api-clients` | GET/POST | list or issue partner API keys |
| `/api/v1/integration/webhooks` | GET/POST | list or register webhook endpoints |
| `/api/v1/integration/webhook-deliveries` | GET | list delivery attempts |
| `/api/v1/integration/webhook-deliveries/dispatch` | POST | trigger pending dispatches |

---

## 5. Authentication and authorization

### Interactive users

Seeded demo credentials:

- `admin / Admin@123`
- `accountant / Accountant@123`
- `approver / Approver@123`
- `integrator / Integrator@123`

Roles:

- `ADMIN`
- `ACCOUNTANT`
- `APPROVER`
- `DEVELOPER_PLATFORM_INTEGRATOR`

### API key auth

Partner API clients authenticate with:

```text
X-API-Key: <raw-api-key>
```

Seeded demo API key:

```text
pk_live_demo_embedded_payroll_partner_2026
```

This authenticates as the seeded partner client for the default tenant.

---

## 6. Idempotency and webhooks

### Idempotency

Key write endpoints support:

```text
Idempotency-Key: <caller-generated-key>
```

The backend stores the request fingerprint and serialized response so repeat submissions with the same key are replayed safely.

### Webhooks

Webhook support includes:

- endpoint registration
- HMAC SHA-256 signing
- delivery queue / outbox persistence
- retry state tracking
- manual or scheduled dispatch

Example webhook headers:

- `X-Webhook-Event`
- `X-Webhook-Event-Key`
- `X-Webhook-Signature`

---

## 7. Temporal workflow support

The project includes Temporal workflow and activity definitions for tax filing orchestration.

### Default development behavior

Temporal is **disabled by default**. In this mode:

- filing workflow requests are still created
- filings are generated through a local development fallback

### Enable Temporal

Configure in `backend/src/main/resources/application.yml`:

```yaml
app:
  temporal:
    enabled: true
    namespace: default
    target: 127.0.0.1:7233
    task-queue: embedded-payroll-tax-filings
```

Then run a Temporal server locally (for example via Temporal CLI, Temporalite, or Docker) before starting the backend.

---

## 8. Frontend / employer dashboard

The frontend is a React + TypeScript dashboard that can be:

- run as a standalone employer console
- embedded as part of another platform
- mined for reusable components from `frontend/src/components/index.ts`

### Dashboard features

- company and jurisdiction visibility
- employee and W-4 maintenance
- payroll calculator
- payroll run processing and approvals
- tax filing records and workflow requests
- partner API client issuance
- webhook management and delivery monitoring
- payroll reporting

---

## 9. Detailed schema

See:

- [`docs/schema.md`](docs/schema.md)
- [`docs/partner-integration.md`](docs/partner-integration.md)

---

## 10. How to build and run

### Prerequisites

- Java 21+
- Node.js 22+

The backend uses the Maven wrapper, so a system Maven install is not required.

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend URLs:

```text
http://localhost:8080
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
http://localhost:8080/v3/api-docs/partner-v1
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

The frontend can connect using:

- username/password
- or a partner API key

---

## 11. Build production artifacts

### Backend JAR

```bash
cd backend
./mvnw clean package
```

Artifact:

```text
backend/target/embedded-payroll-backend-0.0.1-SNAPSHOT.jar
```

Run:

```bash
java -jar backend/target/embedded-payroll-backend-0.0.1-SNAPSHOT.jar
```

### Frontend bundle

```bash
cd frontend
npm install
npm run build
```

Bundle output:

```text
frontend/dist/
```

---

## 12. Verification

Backend:

```bash
cd backend
./mvnw test
./mvnw package -DskipTests
```

Frontend:

```bash
cd frontend
npm run build
```

---

## 13. Deployment guidance

### Recommended deployment model

1. Run the Spring Boot API behind an API gateway / ingress.
2. Deploy the frontend bundle behind a reverse proxy or CDN.
3. Replace the in-memory H2 database with PostgreSQL or another production RDBMS.
4. Put partner APIs behind gateway controls such as rate limiting and request logging.
5. Enable Temporal with a managed/self-hosted cluster for production filing workflows.

### Production hardening checklist

- move from demo Basic Auth to SSO / OAuth2 for interactive users
- rotate and vault API keys / webhook signing secrets
- add stronger tenant isolation at the data and gateway layers
- add payment rails / ACH integration
- add certified filing provider integrations
- add richer state and local tax content maintenance
- add audit, reconciliation, and accounting postings
- add observability and alerting around workflow/webhook failures

---

## 14. Important note on compliance

This project is designed around US payroll and tax norms and now includes:

- W-2-focused payroll processing
- federal withholding and FICA/FUTA modeling
- 50-state seeded tax coverage
- local jurisdiction support
- filing records and workflow orchestration
- partner integration patterns (API keys, idempotency, webhooks)

Before production use, validate against:

- IRS and SSA updates
- each state labor / revenue authority
- local taxing jurisdictions
- payroll accounting policy
- retention and audit requirements
- filing/remittance provider requirements
