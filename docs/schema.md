# Embedded Payroll Schema

This schema backs a **multi-tenant US payroll and tax platform** with:

- company / organization master data
- jurisdiction registrations
- W-2 employee payroll
- federal + state + local tax rules
- payroll runs and detailed run items
- tax filing records and Temporal filing workflow tracking
- partner API clients, idempotency records, and webhook deliveries

## High-level relationships

```text
organizations
  -> user_accounts
     -> user_account_roles
  -> organization_jurisdictions
  -> employees
     -> employee_w4_profiles
  -> payroll_schedules
     -> payroll_runs
        -> payroll_run_items
  -> partner_api_clients
     -> webhook_endpoints
        -> webhook_deliveries
  -> idempotency_records
  -> tax_filing_records
  -> tax_filing_workflow_requests

tax_jurisdictions
  -> jurisdiction_tax_profiles

tax_year_profiles
  -> federal_tax_brackets
```

## Core tenant/company tables

### organizations

Represents a tenant company or employer.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| tenant_key | varchar(64) | unique tenant identifier used for multi-tenant routing / integration |
| code | varchar(32) | organization code |
| legal_name | varchar(160) | legal company name |
| dba_name | varchar(160) | optional doing-business-as name |
| ein | varchar(32) | employer federal tax id |
| default_state_code | varchar(8) | default payroll state |
| country_code | varchar(8) | `US` |
| primary_jurisdiction_code | varchar(32) | primary state jurisdiction |
| headquarters_local_jurisdiction_code | varchar(32) | optional HQ local tax jurisdiction |
| default_currency | varchar(8) | `USD` |
| accounting_method | varchar(40) | accrual/cash-style accounting mode |
| contact_email | varchar(120) | operations contact |
| created_at / updated_at | timestamptz | audit timestamps |

### organization_jurisdictions

Tracks where the company is registered or has payroll nexus.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| organization_id | bigint | FK -> organizations |
| tax_jurisdiction_id | bigint | FK -> tax_jurisdictions |
| nexus_type | varchar(24) | `REGISTERED`, `WORK_LOCATION`, `RESIDENCE`, `BOTH` |
| registration_number | varchar(80) | state/local registration identifier |
| employer_account_number | varchar(80) | employer account or withholding account |
| primary_jurisdiction | boolean | marks primary payroll nexus |
| created_at / updated_at | timestamptz | audit timestamps |

## Security and platform integration

### user_accounts

Interactive users authenticated with username/password.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| organization_id | bigint | FK -> organizations |
| username | varchar(80) | unique login |
| password_hash | varchar(120) | BCrypt hash |
| full_name | varchar(120) | display name |
| email | varchar(160) | contact email |
| active | boolean | active flag |
| created_at / updated_at | timestamptz | audit timestamps |

### user_account_roles

RBAC mapping for interactive users.

Supported roles:

- `ADMIN`
- `ACCOUNTANT`
- `APPROVER`
- `DEVELOPER_PLATFORM_INTEGRATOR`

### partner_api_clients

Programmatic partner/system integrator credentials authenticated with API keys.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| organization_id | bigint | FK -> organizations |
| client_code | varchar(80) | unique partner client code |
| display_name | varchar(160) | friendly name |
| key_prefix | varchar(32) | stored prefix for lookup |
| api_key_hash | varchar(128) | SHA-256 hash of the raw API key |
| scopes | varchar(240) | comma-separated integration scopes |
| description | varchar(240) | descriptive notes |
| active | boolean | active flag |
| created_at / updated_at | timestamptz | audit timestamps |

### idempotency_records

Stores replay-safe responses for create/update endpoints.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| organization_id | bigint | FK -> organizations |
| actor_key | varchar(120) | user or API client identity |
| idempotency_key | varchar(120) | caller-supplied key |
| request_method | varchar(16) | HTTP method |
| request_path | varchar(240) | versioned API route |
| request_hash | varchar(128) | request fingerprint |
| response_status_code | integer | stored result code |
| response_body | clob/text | serialized response payload |
| response_type | varchar(120) | DTO class name |
| created_at / updated_at | timestamptz | audit timestamps |

Unique key:

- `(organization_id, actor_key, idempotency_key)`

### webhook_endpoints

Outbound webhook subscriptions for a tenant or specific partner client.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| organization_id | bigint | FK -> organizations |
| partner_api_client_id | bigint | optional FK -> partner_api_clients |
| name | varchar(80) | endpoint label |
| target_url | varchar(320) | delivery destination |
| subscribed_event | varchar(120) | event name or `*` |
| signing_secret | varchar(120) | HMAC signing secret |
| active | boolean | enabled flag |
| created_at / updated_at | timestamptz | audit timestamps |

### webhook_deliveries

Transactional outbox / delivery log for outbound webhooks.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| webhook_endpoint_id | bigint | FK -> webhook_endpoints |
| event_type | varchar(120) | emitted event name |
| event_key | varchar(120) | stable event identifier |
| payload_json | clob/text | serialized event payload |
| signature | varchar(160) | HMAC SHA-256 signature |
| delivery_status | varchar(24) | `PENDING`, `SENT`, `FAILED`, `DEAD` |
| attempts | integer | retry count |
| next_attempt_at | timestamptz | scheduler retry timestamp |
| delivered_at | timestamptz | success timestamp |
| response_status_code | integer | downstream HTTP code |
| response_body | clob/text | downstream body / error |
| created_at / updated_at | timestamptz | audit timestamps |

## Employee and payroll data

### employees

W-2 employee master record.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| organization_id | bigint | FK -> organizations |
| employee_number | varchar(32) | unique within organization |
| first_name / last_name | varchar(80) | legal name |
| email | varchar(160) | employee email |
| ssn_last_four | varchar(4) | masked SSN storage |
| hire_date | date | employment start |
| employment_status | varchar(24) | `ACTIVE`, `ON_LEAVE`, `TERMINATED` |
| compensation_type | varchar(24) | `SALARIED`, `HOURLY` |
| worker_type | varchar(24) | currently `W2_EMPLOYEE` |
| department | varchar(80) | reporting group |
| work_state | varchar(8) | state sourcing jurisdiction |
| residence_state | varchar(8) | resident state |
| work_local_jurisdiction_code | varchar(32) | optional local work tax code |
| residence_local_jurisdiction_code | varchar(32) | optional local resident tax code |
| annual_salary | decimal(14,2) | salaried compensation |
| hourly_rate | decimal(14,2) | hourly compensation |
| standard_hours_per_period | decimal(8,2) | scheduled hours |
| state_withholding_rate | decimal(6,4) | fallback/manual override |
| created_at / updated_at | timestamptz | audit timestamps |

### employee_w4_profiles

Versioned W-4 elections by employee and tax year.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| employee_id | bigint | FK -> employees |
| tax_year | integer | version year |
| filing_status | varchar(40) | `SINGLE`, `MARRIED_FILING_JOINTLY`, `HEAD_OF_HOUSEHOLD` |
| multiple_jobs | boolean | W-4 step 2 |
| dependents_credit | decimal(14,2) | step 3 |
| other_income | decimal(14,2) | step 4(a) |
| deductions | decimal(14,2) | step 4(b) |
| extra_withholding | decimal(14,2) | step 4(c) |
| exempt_from_withholding | boolean | withholding exemption |
| created_at / updated_at | timestamptz | audit timestamps |

### payroll_schedules

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| organization_id | bigint | FK -> organizations |
| name | varchar(80) | schedule name |
| frequency | varchar(24) | weekly, biweekly, semimonthly, monthly |
| next_pay_date | date | next execution date |
| approval_required | boolean | approval workflow toggle |
| active | boolean | active schedule flag |
| created_at / updated_at | timestamptz | audit timestamps |

### payroll_runs

Atomic payroll execution header.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| payroll_schedule_id | bigint | FK -> payroll_schedules |
| period_start / period_end | date | covered work period |
| pay_date | date | payment date |
| tax_year | integer | rule year used |
| status | varchar(32) | `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `POSTED`, `REJECTED` |
| created_by | varchar(80) | processor |
| approved_by | varchar(80) | approver |
| approved_at | timestamptz | approval timestamp |
| gross_total | decimal(14,2) | total gross |
| tax_total | decimal(14,2) | total employee taxes |
| deduction_total | decimal(14,2) | total pretax deductions |
| net_total | decimal(14,2) | total net |
| created_at / updated_at | timestamptz | audit timestamps |

### payroll_run_items

Per-employee payroll detail. This is the main gross-to-net ledger output.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| payroll_run_id | bigint | FK -> payroll_runs |
| employee_id | bigint | FK -> employees |
| tax_year | integer | tax rule version |
| gross_pay | decimal(14,2) | gross wages |
| bonus_pay | decimal(14,2) | bonus component |
| overtime_pay | decimal(14,2) | overtime component |
| pre_tax_deductions | decimal(14,2) | pretax deductions |
| taxable_wages | decimal(14,2) | post-pretax wages |
| federal_income_tax | decimal(14,2) | FIT withholding |
| social_security_employee_tax | decimal(14,2) | employee FICA OASDI |
| medicare_employee_tax | decimal(14,2) | employee Medicare |
| additional_medicare_employee_tax | decimal(14,2) | additional Medicare |
| state_income_tax | decimal(14,2) | state withholding |
| local_income_tax | decimal(14,2) | local withholding |
| employee_tax_total | decimal(14,2) | total employee taxes |
| employer_social_security_tax | decimal(14,2) | employer FICA OASDI |
| employer_medicare_tax | decimal(14,2) | employer Medicare |
| employer_futa_tax | decimal(14,2) | FUTA |
| employer_state_unemployment_tax | decimal(14,2) | SUTA |
| state_jurisdiction_code | varchar(32) | applied state rule |
| local_jurisdiction_code | varchar(32) | applied local rule |
| net_pay | decimal(14,2) | employee take-home pay |
| created_at / updated_at | timestamptz | audit timestamps |

## Tax rule model

### tax_year_profiles

Federal year-specific rules.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| tax_year | integer | unique year |
| social_security_employee_rate | decimal(6,4) | employee FICA OASDI |
| social_security_employer_rate | decimal(6,4) | employer FICA OASDI |
| social_security_wage_base | decimal(14,2) | annual wage cap |
| medicare_employee_rate | decimal(6,4) | employee Medicare |
| medicare_employer_rate | decimal(6,4) | employer Medicare |
| additional_medicare_rate | decimal(6,4) | additional Medicare rate |
| additional_medicare_threshold | decimal(14,2) | threshold |
| federal_unemployment_rate | decimal(6,4) | FUTA |
| federal_unemployment_wage_base | decimal(14,2) | FUTA wage base |
| default_state_unemployment_rate | decimal(6,4) | default SUTA |
| standard_deduction_single / married_jointly / head_of_household | decimal(14,2) | FIT withholding deductions |
| notes | varchar(240) | rule notes |

### federal_tax_brackets

Marginal FIT bracket table per federal tax year and filing status.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| tax_year_profile_id | bigint | FK -> tax_year_profiles |
| filing_status | varchar(40) | W-4 filing status |
| bracket_order | integer | sequence |
| lower_bound | decimal(14,2) | lower bracket edge |
| upper_bound | decimal(14,2) | optional ceiling |
| rate | decimal(6,4) | marginal rate |

### tax_jurisdictions

Reference table for federal, state, and local jurisdictions.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| code | varchar(32) | unique jurisdiction code (`CA`, `NYC_NY`, etc.) |
| name | varchar(160) | display name |
| jurisdiction_type | varchar(16) | `FEDERAL`, `STATE`, `LOCAL` |
| country_code | varchar(8) | `US` |
| state_code | varchar(8) | owning state for state/local rows |
| parent_jurisdiction_code | varchar(32) | parent code, e.g. a local -> state |
| active | boolean | active flag |

### jurisdiction_tax_profiles

Year-specific rules for state withholding, local withholding, and state unemployment taxes.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| tax_jurisdiction_id | bigint | FK -> tax_jurisdictions |
| tax_year | integer | rule year |
| tax_type | varchar(40) | `STATE_WITHHOLDING`, `LOCAL_WITHHOLDING`, `STATE_UNEMPLOYMENT` |
| resident_rate | decimal(8,4) | resident withholding rate |
| non_resident_rate | decimal(8,4) | nonresident withholding rate |
| employer_rate | decimal(8,4) | employer-side unemployment rate |
| wage_base | decimal(14,2) | optional wage base |
| standard_deduction | decimal(14,2) | optional deduction |
| notes | varchar(240) | explanatory notes |

Unique key:

- `(tax_jurisdiction_id, tax_year, tax_type)`

## Filing orchestration and records

### tax_filing_records

Persisted filing outputs generated directly or via a Temporal workflow.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| organization_id | bigint | FK -> organizations |
| filing_type | varchar(32) | `FORM_941`, `FORM_940`, `FORM_W2`, `FORM_W3`, `STATE_WITHHOLDING`, `LOCAL_WITHHOLDING` |
| tax_year | integer | filing year |
| filing_period | varchar(20) | `Q1`, `Q2`, `Q3`, `Q4`, `ANNUAL` |
| filing_jurisdiction_code | varchar(32) | `US`, state code, or local code |
| due_date | date | due date |
| status | varchar(24) | generated/submitted lifecycle |
| total_wages | decimal(14,2) | summarized wages |
| total_tax | decimal(14,2) | summarized tax liability |
| generated_at | timestamptz | generation timestamp |
| submitted_at | timestamptz | submission timestamp |
| reference_number | varchar(80) | filing reference |
| temporal_workflow_id | varchar(120) | optional Temporal workflow ID |
| temporal_run_id | varchar(120) | optional Temporal run ID |
| created_at / updated_at | timestamptz | audit timestamps |

### tax_filing_workflow_requests

Tracks workflow launches for automated tax filing orchestration.

| Column | Type | Notes |
| --- | --- | --- |
| id | bigint | PK |
| organization_id | bigint | FK -> organizations |
| tax_year | integer | filing year |
| filing_period | varchar(20) | requested period |
| requested_filings | varchar(240) | comma-separated filing types |
| requested_by | varchar(80) | actor that launched the workflow |
| workflow_id | varchar(120) | Temporal workflow ID / fallback ID |
| workflow_run_id | varchar(120) | Temporal run ID when available |
| workflow_status | varchar(24) | `QUEUED`, `STARTED`, `COMPLETED`, `FAILED`, `LOCAL_DEV_FALLBACK` |
| requested_at | timestamptz | request time |
| workflow_message | varchar(240) | status note |
| created_at / updated_at | timestamptz | audit timestamps |

## Coverage notes

- The implementation seeds **all 50 US states** in `tax_jurisdictions`.
- It also seeds sample **local jurisdictions** and supports additional local rows through the same data model.
- Federal withholding remains data-driven through `tax_year_profiles` and `federal_tax_brackets`.
- Payroll writes, filing generation, idempotency records, and webhook outbox entries are designed to occur within transactional service boundaries.
