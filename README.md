# Embedded US Payroll and Tax Processing Platform

Reference implementation of an embeddable payroll and tax processing system for the US market.

The project includes:

- **Java + Spring Boot backend** for payroll APIs, RBAC, tax-year aware payroll calculations, filing records, and seeded demo data.
- **React + TypeScript frontend** with reusable UI components that can be embedded into larger HR, ERP, finance, or workforce platforms.
- **Detailed schema documentation** for employee, W-4, payroll run, tax rule, approval, and filing entities.

> This is a compliance-oriented starter aligned to US payroll concepts (W-4 withholding, FICA/FUTA, approval controls, historical tax years, and filing records). A production rollout still needs authoritative tax table feeds, state/local tax logic, notice processing, and certified filing integrations.

## 1. Solution capabilities

### Payroll and employee management

- Maintain employee master data and compensation setup.
- Store versioned **W-4 tax profiles** by tax year.
- Support salaried and hourly employees.
- Track pay schedules with weekly, biweekly, semimonthly, and monthly frequencies.
- Compute **gross-to-net payroll** with federal withholding, Social Security, Medicare, additional Medicare, FUTA, and state withholding assumptions.

### Approval and RBAC

- Database-backed user accounts with roles:
  - **ADMIN**
  - **ACCOUNTANT**
  - **APPROVER**
- Role-aware API security through Spring Security Basic Auth.
- Payroll runs can enter a **pending approval** state before release.

### Tax engine and historical support

- Seeded tax rule profiles for **2021-2026**, covering the previous five historical tax years plus a current-year reference profile.
- Versioned federal tax brackets by filing status.
- Filing record generation for:
  - **Form 941**
  - **Form 940**
  - **Form W-2**
  - **Form W-3**
  - **State withholding summary**

### Embedded integration model

- Backend exposes REST APIs for partner platform integration.
- Frontend includes reusable components in:
  - `frontend/src/components/index.ts`
- Components can be consumed by another React application or adapted into a library/distribution pipeline.

## 2. Repository structure

```text
.
├── backend/                 Spring Boot payroll API
├── frontend/                React + TypeScript UI shell and embeddable components
├── docs/
│   └── schema.md            Detailed schema and data model documentation
└── README.md
```

## 3. Backend overview

### Main backend modules

- `config/`
  - Spring Security configuration
  - CORS configuration properties
- `controller/`
  - Employee APIs
  - Payroll schedule/run APIs
  - Tax year/filing APIs
  - Dashboard/security APIs
- `model/`
  - Organizations
  - User accounts and roles
  - Employees
  - Employee W-4 profiles
  - Payroll schedules
  - Payroll runs and payroll run items
  - Tax year profiles and federal brackets
  - Tax filing records
- `service/`
  - Tax calculation engine
  - Payroll orchestration
  - Tax filing generation
  - Demo data initializer

### Key API endpoints

| Endpoint | Method | Purpose |
| --- | --- | --- |
| `/api/security/me` | GET | Current authenticated user |
| `/api/dashboard/summary` | GET | Dashboard metrics for an organization |
| `/api/employees` | GET/POST | List or create employees |
| `/api/employees/{id}/w4` | PUT | Upsert W-4 profile |
| `/api/schedules` | GET/POST | List or create payroll schedules |
| `/api/schedules/{id}/process` | POST | Process a scheduled payroll |
| `/api/payroll-runs` | GET | List payroll runs and line items |
| `/api/payroll-runs/{id}/approve` | POST | Approve a payroll run |
| `/api/payroll/calculate` | POST | Ad hoc gross-to-net calculation |
| `/api/tax/years` | GET | Historical tax year support and bracket tables |
| `/api/tax/filings` | GET | List filing records |
| `/api/tax/filings/generate` | POST | Generate filing summary records |

### Demo users

Seeded demo credentials:

- `admin / Admin@123`
- `accountant / Accountant@123`
- `approver / Approver@123`

## 4. Frontend overview

The frontend is a dashboard shell plus reusable components that can be lifted into another product experience.

### Reusable UI components

- `ConnectionPanel`
- `StatCard`
- `RoleBadge`
- `SectionCard`
- `EmployeeTable`
- `PayrollCalculator`
- `PayrollRunTable`
- `TaxFilingTable`
- `TaxYearCard`

All components are exported from:

```ts
frontend/src/components/index.ts
```

## 5. Detailed schema

See:

- [`docs/schema.md`](docs/schema.md)

This document explains the normalized data model, relationships, and tax-year versioning approach.

## 6. How to build and run

### Prerequisites

- Java 21+
- Node.js 22+

No system Maven install is required because the backend includes the Maven wrapper.

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend default URL:

```text
http://localhost:8080
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend default URL:

```text
http://localhost:5173
```

The frontend is preconfigured to connect to `http://localhost:8080` by default, but the connection panel lets you point the UI at any compatible backend deployment.

## 7. How to build production artifacts

### Backend JAR

```bash
cd backend
./mvnw clean package
```

Generated artifact:

```text
backend/target/embedded-payroll-backend-0.0.1-SNAPSHOT.jar
```

Run the packaged backend:

```bash
java -jar backend/target/embedded-payroll-backend-0.0.1-SNAPSHOT.jar
```

### Frontend static bundle

```bash
cd frontend
npm install
npm run build
```

Generated frontend assets:

```text
frontend/dist/
```

## 8. Deployment guidance

### Recommended deployment topology

1. Deploy the Spring Boot service behind an API gateway or ingress.
2. Deploy the React build as static assets behind a CDN or reverse proxy.
3. Place both behind your platform authentication or retain the built-in API authentication layer.
4. Replace H2 with a production database and move seeded demo data to migrations or onboarding flows.
5. Replace seeded tax profiles with a governed regulatory/tax-content update process.

### Suggested production hardening

- Migrate authentication from demo Basic Auth to SSO or OAuth2/OpenID Connect.
- Back the app with PostgreSQL or another production-grade RDBMS.
- Add audit trails, approval evidence, and ledger postings.
- Add state/local tax tables and jurisdiction mapping.
- Integrate ACH/direct deposit and certified tax filing providers.
- Add outbound notifications and payroll close controls.

## 9. Testing

### Backend tests

```bash
cd backend
./mvnw test
```

### Frontend checks

```bash
cd frontend
npm install
npm run build
```

## 10. Compliance framing

This project is designed around US payroll and accounting norms:

- W-4-based withholding capture
- FICA/FUTA aware payroll calculations
- tax-year versioned federal rules
- payroll approval workflow
- filing record generation
- masked SSN handling in UI/API responses

Before production go-live, validate the implementation against:

- IRS publications and annual withholding tables
- SSA wage base updates
- state and local tax agencies
- wage-hour rules
- electronic filing and remittance requirements
- organization-specific accounting, approval, and retention policies