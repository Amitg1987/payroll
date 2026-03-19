# Embedded Payroll Schema

This project models a US payroll and tax platform with RBAC, employee/W-4 maintenance, gross-to-net payroll calculations, scheduled payroll runs, approvals, and tax filing records for the last five historical tax years plus a seeded current year profile.

## Core design goals

- Support embeddable API consumption from partner platforms.
- Provide reusable React UI components on top of the same API contract.
- Preserve a normalized schema for payroll master data, payroll execution, tax rules, and filing records.
- Keep tax-year profiles versioned so historical payroll calculations can be reproduced.

## Entity relationship summary

```text
organizations
  -> user_accounts
     -> user_account_roles
  -> employees
     -> employee_w4_profiles
  -> payroll_schedules
     -> payroll_runs
        -> payroll_run_items
  -> tax_filing_records

tax_year_profiles
  -> federal_tax_brackets
```

## Tables

### organizations

| Column | Type | Constraints | Purpose |
| --- | --- | --- | --- |
| id | bigint | PK | Surrogate identifier |
| code | varchar(32) | unique, not null | Partner/platform organization code |
| legal_name | varchar(160) | not null | Registered entity name |
| dba_name | varchar(160) | nullable | Doing-business-as name |
| ein | varchar(32) | unique, not null | Federal employer identification number |
| default_state_code | varchar(8) | not null | Primary tax nexus state |
| default_currency | varchar(8) | not null | USD by default |
| accounting_method | varchar(40) | not null | Cash vs accrual reporting orientation |
| contact_email | varchar(120) | not null | Payroll operations contact |
| created_at / updated_at | timestamptz | not null | Audit timestamps |

### user_accounts

| Column | Type | Constraints | Purpose |
| --- | --- | --- | --- |
| id | bigint | PK | User identifier |
| organization_id | bigint | FK -> organizations.id, not null | Org scoping for RBAC |
| username | varchar(80) | unique, not null | Login name |
| password_hash | varchar(120) | not null | BCrypt credential hash |
| full_name | varchar(120) | not null | Display name |
| email | varchar(160) | not null | Contact and notification email |
| active | boolean | not null | Soft-enable or disable account |
| created_at / updated_at | timestamptz | not null | Audit timestamps |

### user_account_roles

| Column | Type | Constraints | Purpose |
| --- | --- | --- | --- |
| user_account_id | bigint | FK -> user_accounts.id | Role owner |
| role_name | varchar(40) | not null | `ADMIN`, `ACCOUNTANT`, or `APPROVER` |

This supports RBAC without hard-coding roles in the UI.

### employees

| Column | Type | Constraints | Purpose |
| --- | --- | --- | --- |
| id | bigint | PK | Employee identifier |
| organization_id | bigint | FK -> organizations.id, not null | Employer relationship |
| employee_number | varchar(32) | unique per organization | HRIS/payroll cross-reference |
| first_name / last_name | varchar(80) | not null | Legal payroll name |
| email | varchar(160) | not null | Work email |
| ssn_last_four | varchar(4) | not null | Masked SSN tracking |
| hire_date | date | not null | Service start date |
| employment_status | varchar(24) | not null | `ACTIVE`, `ON_LEAVE`, `TERMINATED` |
| compensation_type | varchar(24) | not null | `SALARIED` or `HOURLY` |
| department | varchar(80) | nullable | Cost center / reporting dimension |
| work_state | varchar(8) | not null | Work-state sourcing for tax withholding |
| annual_salary | decimal(14,2) | nullable | Used for salaried workers |
| hourly_rate | decimal(14,2) | nullable | Used for hourly workers |
| standard_hours_per_period | decimal(8,2) | not null | Scheduled hours used by payroll engine |
| state_withholding_rate | decimal(6,4) | not null | Configurable per-employee state withholding assumption |
| created_at / updated_at | timestamptz | not null | Audit timestamps |

### employee_w4_profiles

| Column | Type | Constraints | Purpose |
| --- | --- | --- | --- |
| id | bigint | PK | W-4 profile identifier |
| employee_id | bigint | FK -> employees.id, not null | Employee linkage |
| tax_year | integer | unique per employee/year | Versioned W-4 profile |
| filing_status | varchar(40) | not null | `SINGLE`, `MARRIED_FILING_JOINTLY`, `HEAD_OF_HOUSEHOLD` |
| multiple_jobs | boolean | not null | W-4 step 2 indicator |
| dependents_credit | decimal(14,2) | not null | Step 3 annual tax credit |
| other_income | decimal(14,2) | not null | Step 4(a) |
| deductions | decimal(14,2) | not null | Step 4(b) |
| extra_withholding | decimal(14,2) | not null | Step 4(c) extra per-period withholding |
| exempt_from_withholding | boolean | not null | Federal withholding exemption flag |
| created_at / updated_at | timestamptz | not null | Audit timestamps |

### payroll_schedules

| Column | Type | Constraints | Purpose |
| --- | --- | --- | --- |
| id | bigint | PK | Schedule identifier |
| organization_id | bigint | FK -> organizations.id, not null | Org ownership |
| name | varchar(80) | not null | Human-readable schedule name |
| frequency | varchar(24) | not null | Weekly, biweekly, semimonthly, monthly |
| next_pay_date | date | not null | Next schedule execution target |
| approval_required | boolean | not null | Whether runs enter approval queue |
| active | boolean | not null | Toggle schedule availability |
| created_at / updated_at | timestamptz | not null | Audit timestamps |

### payroll_runs

| Column | Type | Constraints | Purpose |
| --- | --- | --- | --- |
| id | bigint | PK | Payroll run identifier |
| payroll_schedule_id | bigint | FK -> payroll_schedules.id, not null | Source schedule |
| period_start / period_end | date | not null | Earnings period represented |
| pay_date | date | not null | Check/direct-deposit date |
| tax_year | integer | not null | Tax-year profile used for withholding |
| status | varchar(32) | not null | `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `POSTED`, `REJECTED` |
| created_by | varchar(80) | not null | Processor username |
| approved_by | varchar(80) | nullable | Approver username |
| approved_at | timestamptz | nullable | Approval timestamp |
| gross_total | decimal(14,2) | not null | Aggregate gross wages |
| tax_total | decimal(14,2) | not null | Aggregate employee taxes |
| deduction_total | decimal(14,2) | not null | Aggregate pre-tax deductions |
| net_total | decimal(14,2) | not null | Aggregate net pay |
| created_at / updated_at | timestamptz | not null | Audit timestamps |

### payroll_run_items

| Column | Type | Constraints | Purpose |
| --- | --- | --- | --- |
| id | bigint | PK | Line item identifier |
| payroll_run_id | bigint | FK -> payroll_runs.id, not null | Parent run |
| employee_id | bigint | FK -> employees.id, not null | Employee being paid |
| tax_year | integer | not null | Rule version used |
| gross_pay | decimal(14,2) | not null | Total wages before deductions |
| bonus_pay | decimal(14,2) | not null | Supplemental compensation component |
| overtime_pay | decimal(14,2) | not null | Overtime premium |
| pre_tax_deductions | decimal(14,2) | not null | Pretax deduction input |
| taxable_wages | decimal(14,2) | not null | Wages exposed to tax engine |
| federal_income_tax | decimal(14,2) | not null | Federal withholding |
| social_security_employee_tax | decimal(14,2) | not null | Employee OASDI |
| medicare_employee_tax | decimal(14,2) | not null | Employee Medicare |
| additional_medicare_employee_tax | decimal(14,2) | not null | Additional Medicare withholding |
| state_income_tax | decimal(14,2) | not null | Simplified state withholding |
| employee_tax_total | decimal(14,2) | not null | Sum of employee tax obligations |
| employer_social_security_tax | decimal(14,2) | not null | Employer OASDI |
| employer_medicare_tax | decimal(14,2) | not null | Employer Medicare |
| employer_futa_tax | decimal(14,2) | not null | Employer FUTA |
| net_pay | decimal(14,2) | not null | Net pay after deductions and taxes |
| created_at / updated_at | timestamptz | not null | Audit timestamps |

### tax_year_profiles

| Column | Type | Constraints | Purpose |
| --- | --- | --- | --- |
| id | bigint | PK | Tax profile identifier |
| tax_year | integer | unique, not null | Version key |
| social_security_employee_rate | decimal(6,4) | not null | Employee OASDI rate |
| social_security_employer_rate | decimal(6,4) | not null | Employer OASDI rate |
| social_security_wage_base | decimal(14,2) | not null | Annual wage cap |
| medicare_employee_rate | decimal(6,4) | not null | Employee Medicare rate |
| medicare_employer_rate | decimal(6,4) | not null | Employer Medicare rate |
| additional_medicare_rate | decimal(6,4) | not null | Additional Medicare rate |
| additional_medicare_threshold | decimal(14,2) | not null | Threshold for extra Medicare withholding |
| federal_unemployment_rate | decimal(6,4) | not null | FUTA rate assumption |
| federal_unemployment_wage_base | decimal(14,2) | not null | FUTA wage cap |
| default_state_unemployment_rate | decimal(6,4) | not null | Reference SUTA assumption |
| standard_deduction_single | decimal(14,2) | not null | Standard deduction for single filers |
| standard_deduction_married_jointly | decimal(14,2) | not null | Standard deduction for MFJ |
| standard_deduction_head_of_household | decimal(14,2) | not null | Standard deduction for HOH |
| notes | varchar(240) | not null | Regulatory/version note |
| created_at / updated_at | timestamptz | not null | Audit timestamps |

### federal_tax_brackets

| Column | Type | Constraints | Purpose |
| --- | --- | --- | --- |
| id | bigint | PK | Bracket identifier |
| tax_year_profile_id | bigint | FK -> tax_year_profiles.id, not null | Parent tax-year profile |
| filing_status | varchar(40) | not null | Filing status to which bracket applies |
| bracket_order | integer | not null | Sort order |
| lower_bound | decimal(14,2) | not null | Inclusive lower bound |
| upper_bound | decimal(14,2) | nullable | Inclusive ceiling; null for top bracket |
| rate | decimal(6,4) | not null | Marginal tax rate |
| created_at / updated_at | timestamptz | not null | Audit timestamps |

### tax_filing_records

| Column | Type | Constraints | Purpose |
| --- | --- | --- | --- |
| id | bigint | PK | Filing record identifier |
| organization_id | bigint | FK -> organizations.id, not null | Filing employer |
| filing_type | varchar(32) | not null | `FORM_941`, `FORM_940`, `FORM_W2`, `FORM_W3`, `STATE_WITHHOLDING` |
| tax_year | integer | not null | Filing year |
| filing_period | varchar(20) | not null | Quarterly or annual period key |
| due_date | date | not null | Expected filing deadline |
| status | varchar(24) | not null | `GENERATED`, `SUBMITTED`, `ACCEPTED`, `REJECTED` |
| total_wages | decimal(14,2) | not null | Wages summarized for filing |
| total_tax | decimal(14,2) | not null | Tax liability summarized for filing |
| generated_at | timestamptz | not null | Record creation timestamp |
| submitted_at | timestamptz | nullable | Submission timestamp |
| reference_number | varchar(80) | nullable | Provider or internal reference |
| created_at / updated_at | timestamptz | not null | Audit timestamps |

## Historical tax support

The seeded `tax_year_profiles` cover 2021-2026. That allows:

- recalculating payroll for the previous five tax years,
- storing historically correct W-4 profiles by year,
- generating year-specific filing records,
- maintaining bracketed federal withholding logic by tax year.

## Compliance note

The schema is aligned to US payroll concepts (W-4 withholding inputs, FICA/FUTA fields, approval controls, filing artifacts, and tax-year versioning). Production deployments should still layer in authoritative tax table feeds, state/local jurisdiction data, notice handling, and certified e-file integrations before legal go-live.
