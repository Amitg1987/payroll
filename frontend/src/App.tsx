import { useEffect, useMemo, useState } from 'react'
import { payrollApi } from './api/client'
import type {
  ConnectionSettings,
  CreateEmployeeRequest,
  CurrentUser,
  DashboardSummary,
  Employee,
  GenerateFilingRequest,
  PayrollCalculationRequest,
  PayrollCalculationResult,
  PayrollRun,
  PayrollSchedule,
  RoleName,
  TaxFilingRecord,
  TaxYearProfile,
} from './api/types'
import {
  ConnectionPanel,
  EmployeeTable,
  PayrollCalculator,
  PayrollRunTable,
  RoleBadge,
  SectionCard,
  StatCard,
  TaxFilingTable,
  TaxYearCard,
} from './components'
import { formatDate, titleCase } from './lib/formatters'

const DEFAULT_SETTINGS: ConnectionSettings = {
  baseUrl: 'http://localhost:8080',
  username: 'admin',
  password: 'Admin@123',
}

function createEmployeeDraft(
  organizationId: number,
  taxYear: number,
): CreateEmployeeRequest {
  return {
    organizationId,
    employeeNumber: '',
    firstName: '',
    lastName: '',
    email: '',
    ssnLastFour: '',
    hireDate: new Date().toISOString().slice(0, 10),
    employmentStatus: 'ACTIVE',
    compensationType: 'SALARIED',
    department: 'Operations',
    workState: 'CA',
    annualSalary: 90000,
    hourlyRate: null,
    standardHoursPerPeriod: 80,
    stateWithholdingRate: 0.05,
    w4Profile: {
      taxYear,
      filingStatus: 'SINGLE',
      multipleJobs: false,
      dependentsCredit: 0,
      otherIncome: 0,
      deductions: 0,
      extraWithholding: 0,
      exemptFromWithholding: false,
    },
  }
}

function App() {
  const [settings, setSettings] = useState<ConnectionSettings>(DEFAULT_SETTINGS)
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null)
  const [dashboard, setDashboard] = useState<DashboardSummary | null>(null)
  const [employees, setEmployees] = useState<Employee[]>([])
  const [schedules, setSchedules] = useState<PayrollSchedule[]>([])
  const [runs, setRuns] = useState<PayrollRun[]>([])
  const [taxYears, setTaxYears] = useState<TaxYearProfile[]>([])
  const [filings, setFilings] = useState<TaxFilingRecord[]>([])
  const [employeeDraft, setEmployeeDraft] = useState<CreateEmployeeRequest>(
    createEmployeeDraft(0, new Date().getFullYear()),
  )
  const [calculatorForm, setCalculatorForm] = useState<PayrollCalculationRequest>({
    employeeId: 0,
    taxYear: new Date().getFullYear(),
    frequency: 'BIWEEKLY',
    bonusPay: 0,
    overtimeHours: 0,
    preTaxDeductions: 0,
  })
  const [calculatorResult, setCalculatorResult] =
    useState<PayrollCalculationResult | null>(null)
  const [filingForm, setFilingForm] = useState<GenerateFilingRequest>({
    organizationId: 0,
    taxYear: new Date().getFullYear(),
    filingType: 'FORM_941',
    filingPeriod: 'Q1',
  })
  const [loading, setLoading] = useState(false)
  const [submittingEmployee, setSubmittingEmployee] = useState(false)
  const [processingScheduleId, setProcessingScheduleId] = useState<number | null>(null)
  const [approvingRunId, setApprovingRunId] = useState<number | null>(null)
  const [generatingFiling, setGeneratingFiling] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const canEdit = hasRole(currentUser?.roles, ['ADMIN', 'ACCOUNTANT'])
  const canApprove = hasRole(currentUser?.roles, ['ADMIN', 'APPROVER'])
  const selectedTaxProfile = useMemo(
    () => taxYears.find((profile) => profile.taxYear === filingForm.taxYear) ?? null,
    [filingForm.taxYear, taxYears],
  )

  async function loadWorkspace(activeSettings: ConnectionSettings = settings) {
    setLoading(true)
    setError(null)

    try {
      const me = await payrollApi.currentUser(activeSettings)
      const [dashboardData, employeeData, scheduleData, runData, taxYearData] =
        await Promise.all([
          payrollApi.dashboard(activeSettings, me.organizationId),
          payrollApi.employees(activeSettings, me.organizationId),
          payrollApi.schedules(activeSettings, me.organizationId),
          payrollApi.payrollRuns(activeSettings, me.organizationId),
          payrollApi.taxYears(activeSettings),
        ])

      const preferredTaxYear =
        taxYearData.find((profile) => profile.taxYear === filingForm.taxYear)?.taxYear ??
        taxYearData[0]?.taxYear ??
        new Date().getFullYear()
      const filingData = await payrollApi.filings(
        activeSettings,
        me.organizationId,
        preferredTaxYear,
      )

      setCurrentUser(me)
      setDashboard(dashboardData)
      setEmployees(employeeData)
      setSchedules(scheduleData)
      setRuns(runData)
      setTaxYears(taxYearData)
      setFilings(filingData)
      setCalculatorForm((previous) => ({
        ...previous,
        employeeId: previous.employeeId || employeeData[0]?.id || 0,
        frequency: scheduleData[0]?.frequency ?? previous.frequency,
        taxYear: preferredTaxYear,
      }))
      setEmployeeDraft((previous) => ({
        ...createEmployeeDraft(me.organizationId, preferredTaxYear),
        compensationType: previous.compensationType,
      }))
      setFilingForm((previous) => ({
        ...previous,
        organizationId: me.organizationId,
        taxYear: preferredTaxYear,
      }))
    } catch (unknownError) {
      setError(
        unknownError instanceof Error
          ? unknownError.message
          : 'Unable to connect to the payroll backend.',
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadWorkspace(DEFAULT_SETTINGS)
    // Initial load should use the built-in demo credentials.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!currentUser) {
      return
    }

    void payrollApi
      .filings(settings, currentUser.organizationId, filingForm.taxYear)
      .then(setFilings)
      .catch((unknownError) => {
        setError(
          unknownError instanceof Error
            ? unknownError.message
            : 'Unable to refresh filing records.',
        )
      })
  }, [currentUser, filingForm.taxYear, settings])

  async function handleCreateEmployee() {
    if (!currentUser) {
      return
    }
    setSubmittingEmployee(true)
    setError(null)
    setNotice(null)

    try {
      await payrollApi.createEmployee(settings, {
        ...employeeDraft,
        organizationId: currentUser.organizationId,
        annualSalary:
          employeeDraft.compensationType === 'SALARIED'
            ? employeeDraft.annualSalary
            : null,
        hourlyRate:
          employeeDraft.compensationType === 'HOURLY'
            ? employeeDraft.hourlyRate
            : null,
      })
      setNotice('Employee and W-4 profile created.')
      setEmployeeDraft(createEmployeeDraft(currentUser.organizationId, filingForm.taxYear))
      await loadWorkspace()
    } catch (unknownError) {
      setError(
        unknownError instanceof Error
          ? unknownError.message
          : 'Unable to create employee.',
      )
    } finally {
      setSubmittingEmployee(false)
    }
  }

  async function handleCalculatePayroll() {
    setError(null)
    setNotice(null)

    try {
      const result = await payrollApi.calculatePayroll(settings, calculatorForm)
      setCalculatorResult(result)
      setNotice('Gross-to-net calculation completed.')
    } catch (unknownError) {
      setError(
        unknownError instanceof Error
          ? unknownError.message
          : 'Unable to calculate payroll.',
      )
    }
  }

  async function handleProcessSchedule(scheduleId: number) {
    setProcessingScheduleId(scheduleId)
    setError(null)
    setNotice(null)

    try {
      await payrollApi.processSchedule(settings, scheduleId)
      setNotice('Scheduled payroll processed successfully.')
      await loadWorkspace()
    } catch (unknownError) {
      setError(
        unknownError instanceof Error
          ? unknownError.message
          : 'Unable to process scheduled payroll.',
      )
    } finally {
      setProcessingScheduleId(null)
    }
  }

  async function handleApproveRun(runId: number) {
    setApprovingRunId(runId)
    setError(null)
    setNotice(null)

    try {
      await payrollApi.approvePayrollRun(settings, runId)
      setNotice('Payroll run approved.')
      await loadWorkspace()
    } catch (unknownError) {
      setError(
        unknownError instanceof Error
          ? unknownError.message
          : 'Unable to approve payroll run.',
      )
    } finally {
      setApprovingRunId(null)
    }
  }

  async function handleGenerateFiling() {
    setGeneratingFiling(true)
    setError(null)
    setNotice(null)

    try {
      await payrollApi.generateFiling(settings, filingForm)
      setNotice('Tax filing record generated.')
      if (currentUser) {
        setFilings(
          await payrollApi.filings(
            settings,
            currentUser.organizationId,
            filingForm.taxYear,
          ),
        )
      }
    } catch (unknownError) {
      setError(
        unknownError instanceof Error
          ? unknownError.message
          : 'Unable to generate filing record.',
      )
    } finally {
      setGeneratingFiling(false)
    }
  }

  return (
    <div className="app-shell">
      <header className="hero-shell">
        <div>
          <p className="eyebrow">Embedded Payroll Platform</p>
          <h1>US payroll, tax, approvals, and historical filing support</h1>
          <p className="hero-copy">
            Spring Boot APIs and React UI components for employee onboarding,
            W-4 capture, gross-to-net calculation, schedule-driven payroll
            runs, approval routing, and tax-year aware filing records.
          </p>
        </div>
        <div className="hero-panel">
          <p className="eyebrow">Current user</p>
          {currentUser ? (
            <>
              <strong>{currentUser.fullName}</strong>
              <span>{currentUser.email}</span>
              <div className="badge-row">
                {currentUser.roles.map((role) => (
                  <RoleBadge key={role} role={role} />
                ))}
              </div>
            </>
          ) : (
            <span className="muted">Connect to the backend to load demo users.</span>
          )}
        </div>
      </header>

      <SectionCard
        title="Integration connection"
        subtitle="Point the UI components at any compatible backend host."
      >
        <ConnectionPanel
          settings={settings}
          onChange={setSettings}
          onConnect={() => void loadWorkspace()}
          loading={loading}
        />
        {error ? <div className="message message--error">{error}</div> : null}
        {notice ? <div className="message message--success">{notice}</div> : null}
      </SectionCard>

      <div className="stats-grid">
        <StatCard
          label="Organization"
          value={dashboard?.organizationName ?? 'Not connected'}
          hint={dashboard ? `EIN ${dashboard.ein}` : 'Connect to backend'}
        />
        <StatCard
          label="Employees"
          value={dashboard?.employeeCount ?? 0}
          hint="Active employee master profiles"
        />
        <StatCard
          label="Pending approvals"
          value={dashboard?.pendingApprovalRuns ?? 0}
          hint="Payroll runs waiting for approver sign-off"
        />
        <StatCard
          label="Next pay date"
          value={dashboard ? formatDate(dashboard.nextPayDate) : 'Not scheduled'}
          hint="Driven from payroll schedules"
        />
      </div>

      <div className="page-grid">
        <SectionCard
          title="Employee master data and W-4 maintenance"
          subtitle="Maintains employee profile, compensation basis, state work location, and federal W-4 information."
        >
          {canEdit ? (
            <div className="form-stack">
              <div className="field-grid">
                <label>
                  Employee number
                  <input
                    value={employeeDraft.employeeNumber}
                    onChange={(event) =>
                      setEmployeeDraft({
                        ...employeeDraft,
                        employeeNumber: event.target.value,
                      })
                    }
                  />
                </label>
                <label>
                  First name
                  <input
                    value={employeeDraft.firstName}
                    onChange={(event) =>
                      setEmployeeDraft({
                        ...employeeDraft,
                        firstName: event.target.value,
                      })
                    }
                  />
                </label>
                <label>
                  Last name
                  <input
                    value={employeeDraft.lastName}
                    onChange={(event) =>
                      setEmployeeDraft({
                        ...employeeDraft,
                        lastName: event.target.value,
                      })
                    }
                  />
                </label>
                <label>
                  Email
                  <input
                    value={employeeDraft.email}
                    onChange={(event) =>
                      setEmployeeDraft({
                        ...employeeDraft,
                        email: event.target.value,
                      })
                    }
                  />
                </label>
                <label>
                  SSN last four
                  <input
                    value={employeeDraft.ssnLastFour}
                    onChange={(event) =>
                      setEmployeeDraft({
                        ...employeeDraft,
                        ssnLastFour: event.target.value,
                      })
                    }
                  />
                </label>
                <label>
                  Department
                  <input
                    value={employeeDraft.department}
                    onChange={(event) =>
                      setEmployeeDraft({
                        ...employeeDraft,
                        department: event.target.value,
                      })
                    }
                  />
                </label>
                <label>
                  Compensation type
                  <select
                    value={employeeDraft.compensationType}
                    onChange={(event) =>
                      setEmployeeDraft({
                        ...employeeDraft,
                        compensationType: event.target.value as CreateEmployeeRequest['compensationType'],
                      })
                    }
                  >
                    <option value="SALARIED">Salaried</option>
                    <option value="HOURLY">Hourly</option>
                  </select>
                </label>
                <label>
                  Annual salary
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={employeeDraft.annualSalary ?? 0}
                    onChange={(event) =>
                      setEmployeeDraft({
                        ...employeeDraft,
                        annualSalary: Number(event.target.value),
                      })
                    }
                    disabled={employeeDraft.compensationType !== 'SALARIED'}
                  />
                </label>
                <label>
                  Hourly rate
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={employeeDraft.hourlyRate ?? 0}
                    onChange={(event) =>
                      setEmployeeDraft({
                        ...employeeDraft,
                        hourlyRate: Number(event.target.value),
                      })
                    }
                    disabled={employeeDraft.compensationType !== 'HOURLY'}
                  />
                </label>
                <label>
                  Filing status
                  <select
                    value={employeeDraft.w4Profile.filingStatus}
                    onChange={(event) =>
                      setEmployeeDraft({
                        ...employeeDraft,
                        w4Profile: {
                          ...employeeDraft.w4Profile,
                          filingStatus: event.target.value as CreateEmployeeRequest['w4Profile']['filingStatus'],
                        },
                      })
                    }
                  >
                    <option value="SINGLE">Single</option>
                    <option value="MARRIED_FILING_JOINTLY">
                      Married filing jointly
                    </option>
                    <option value="HEAD_OF_HOUSEHOLD">Head of household</option>
                  </select>
                </label>
                <label>
                  Tax year
                  <input
                    type="number"
                    value={employeeDraft.w4Profile.taxYear}
                    onChange={(event) =>
                      setEmployeeDraft({
                        ...employeeDraft,
                        w4Profile: {
                          ...employeeDraft.w4Profile,
                          taxYear: Number(event.target.value),
                        },
                      })
                    }
                  />
                </label>
              </div>
              <button
                type="button"
                onClick={() => void handleCreateEmployee()}
                disabled={submittingEmployee}
              >
                {submittingEmployee ? 'Saving...' : 'Create employee'}
              </button>
            </div>
          ) : (
            <p className="muted">
              Current role is read-only for employee maintenance.
            </p>
          )}
          <EmployeeTable employees={employees} />
        </SectionCard>

        <SectionCard
          title="Gross-to-net calculator"
          subtitle="Uses employee W-4, payroll frequency, FICA rules, and historical tax-year tables."
        >
          <PayrollCalculator
            employees={employees}
            schedules={schedules}
            form={calculatorForm}
            onChange={setCalculatorForm}
            onSubmit={() => void handleCalculatePayroll()}
            result={calculatorResult}
            loading={loading}
          />
        </SectionCard>
      </div>

      <div className="page-grid">
        <SectionCard
          title="Scheduled payroll processing"
          subtitle="Processes gross-to-net runs on defined schedules and moves them into approval."
        >
          <div className="schedule-list">
            {schedules.map((schedule) => (
              <article key={schedule.id} className="schedule-card">
                <div>
                  <h3>{schedule.name}</h3>
                  <p className="muted">
                    {titleCase(schedule.frequency)} • Next pay date{' '}
                    {formatDate(schedule.nextPayDate)}
                  </p>
                </div>
                {canEdit ? (
                  <button
                    type="button"
                    onClick={() => void handleProcessSchedule(schedule.id)}
                    disabled={processingScheduleId === schedule.id}
                  >
                    {processingScheduleId === schedule.id
                      ? 'Processing...'
                      : 'Process payroll'}
                  </button>
                ) : null}
              </article>
            ))}
          </div>
        </SectionCard>

        <SectionCard
          title="Tax year rule support"
          subtitle="Historical engine profiles expose standard deductions, FICA parameters, and federal tax brackets for the last five years plus the seeded current year."
        >
          <div className="tax-year-selector">
            <label>
              Tax year
              <select
                value={filingForm.taxYear}
                onChange={(event) =>
                  setFilingForm({
                    ...filingForm,
                    taxYear: Number(event.target.value),
                  })
                }
              >
                {dashboard?.supportedTaxYears.map((taxYear) => (
                  <option key={taxYear} value={taxYear}>
                    {taxYear}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <TaxYearCard profile={selectedTaxProfile} />
        </SectionCard>
      </div>

      <SectionCard
        title="Payroll runs and approval queue"
        subtitle="Admin and approver roles can release runs after review; accountants can process new cycles."
      >
        <PayrollRunTable
          runs={runs}
          canApprove={canApprove}
          approvingRunId={approvingRunId}
          onApprove={(runId) => void handleApproveRun(runId)}
        />
      </SectionCard>

      <SectionCard
        title="Tax filing records"
        subtitle="Generate filing-ready summaries for 941, 940, W-2/W-3, and state withholding across historical tax years."
      >
        <TaxFilingTable
          filings={filings}
          form={filingForm}
          onChange={setFilingForm}
          onGenerate={() => void handleGenerateFiling()}
          generating={generatingFiling}
          canGenerate={canEdit}
        />
      </SectionCard>
    </div>
  )
}

function hasRole(
  roles: RoleName[] | undefined,
  requiredRoles: RoleName[],
): boolean {
  return requiredRoles.some((role) => roles?.includes(role))
}

export default App
