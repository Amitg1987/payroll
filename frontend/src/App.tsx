import { useEffect, useMemo, useState } from 'react'
import { payrollApi } from './api/client'
import type {
  ConnectionSettings,
  CreateApiClientRequest,
  CreateEmployeeRequest,
  CreateWebhookRequest,
  CurrentUser,
  DashboardSummary,
  Employee,
  FilingWorkflowRequest,
  GenerateFilingRequest,
  OrganizationProfile,
  PartnerApiClient,
  PayrollCalculationRequest,
  PayrollCalculationResult,
  PayrollRun,
  PayrollSchedule,
  PayrollSummaryReport,
  RoleName,
  StartFilingWorkflowRequest,
  TaxFilingRecord,
  TaxJurisdiction,
  TaxYearProfile,
  WebhookDelivery,
  WebhookEndpoint,
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
import { formatCurrency, formatDate, titleCase } from './lib/formatters'

const DEFAULT_SETTINGS: ConnectionSettings = {
  baseUrl: 'http://localhost:8080',
  username: 'admin',
  password: 'Admin@123',
  apiKey: '',
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
    residenceState: 'CA',
    workLocalJurisdictionCode: 'SF_CA',
    residenceLocalJurisdictionCode: 'SF_CA',
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

function createApiClientDraft(organizationId: number): CreateApiClientRequest {
  return {
    organizationId,
    clientCode: '',
    displayName: '',
    scopes: 'employees:write,payroll:write,reports:read,webhooks:manage,tax:write',
    description: '',
  }
}

function createWebhookDraft(organizationId: number): CreateWebhookRequest {
  return {
    organizationId,
    partnerApiClientId: null,
    name: '',
    targetUrl: '',
    subscribedEvent: '*',
  }
}

function createWorkflowDraft(organizationId: number, taxYear: number): StartFilingWorkflowRequest {
  return {
    organizationId,
    taxYear,
    filingPeriod: 'ANNUAL',
    filingTypes: ['FORM_W2', 'FORM_W3', 'STATE_WITHHOLDING', 'LOCAL_WITHHOLDING'],
  }
}

function App() {
  const [settings, setSettings] = useState<ConnectionSettings>(DEFAULT_SETTINGS)
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null)
  const [dashboard, setDashboard] = useState<DashboardSummary | null>(null)
  const [organization, setOrganization] = useState<OrganizationProfile | null>(null)
  const [employees, setEmployees] = useState<Employee[]>([])
  const [schedules, setSchedules] = useState<PayrollSchedule[]>([])
  const [runs, setRuns] = useState<PayrollRun[]>([])
  const [taxYears, setTaxYears] = useState<TaxYearProfile[]>([])
  const [jurisdictions, setJurisdictions] = useState<TaxJurisdiction[]>([])
  const [filings, setFilings] = useState<TaxFilingRecord[]>([])
  const [workflows, setWorkflows] = useState<FilingWorkflowRequest[]>([])
  const [report, setReport] = useState<PayrollSummaryReport | null>(null)
  const [apiClients, setApiClients] = useState<PartnerApiClient[]>([])
  const [webhooks, setWebhooks] = useState<WebhookEndpoint[]>([])
  const [deliveries, setDeliveries] = useState<WebhookDelivery[]>([])
  const [employeeDraft, setEmployeeDraft] = useState<CreateEmployeeRequest>(
    createEmployeeDraft(0, new Date().getFullYear()),
  )
  const [apiClientDraft, setApiClientDraft] = useState<CreateApiClientRequest>(
    createApiClientDraft(0),
  )
  const [webhookDraft, setWebhookDraft] = useState<CreateWebhookRequest>(
    createWebhookDraft(0),
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
    filingJurisdictionCode: null,
  })
  const [workflowDraft, setWorkflowDraft] = useState<StartFilingWorkflowRequest>(
    createWorkflowDraft(0, new Date().getFullYear()),
  )
  const [loading, setLoading] = useState(false)
  const [submittingEmployee, setSubmittingEmployee] = useState(false)
  const [processingScheduleId, setProcessingScheduleId] = useState<number | null>(null)
  const [approvingRunId, setApprovingRunId] = useState<number | null>(null)
  const [generatingFiling, setGeneratingFiling] = useState(false)
  const [startingWorkflow, setStartingWorkflow] = useState(false)
  const [creatingApiClient, setCreatingApiClient] = useState(false)
  const [creatingWebhook, setCreatingWebhook] = useState(false)
  const [dispatchingWebhooks, setDispatchingWebhooks] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const canEdit = hasRole(currentUser?.roles, [
    'ADMIN',
    'ACCOUNTANT',
    'DEVELOPER_PLATFORM_INTEGRATOR',
  ])
  const canApprove = hasRole(currentUser?.roles, ['ADMIN', 'APPROVER'])
  const canManageIntegrations = hasRole(currentUser?.roles, [
    'ADMIN',
    'DEVELOPER_PLATFORM_INTEGRATOR',
  ])
  const selectedTaxProfile = useMemo(
    () => taxYears.find((profile) => profile.taxYear === filingForm.taxYear) ?? null,
    [filingForm.taxYear, taxYears],
  )

  async function loadWorkspace(activeSettings: ConnectionSettings = settings) {
    setLoading(true)
    setError(null)

    try {
      const me = await payrollApi.currentUser(activeSettings)
      const integrationEnabled = hasRole(me.roles, [
        'ADMIN',
        'DEVELOPER_PLATFORM_INTEGRATOR',
      ])
      const [
        dashboardData,
        organizationData,
        employeeData,
        scheduleData,
        runData,
        taxYearData,
        jurisdictionData,
        apiClientData,
        webhookData,
        deliveryData,
        workflowData,
      ] = await Promise.all([
        payrollApi.dashboard(activeSettings, me.organizationId),
        payrollApi.currentOrganization(activeSettings),
        payrollApi.employees(activeSettings, me.organizationId),
        payrollApi.schedules(activeSettings, me.organizationId),
        payrollApi.payrollRuns(activeSettings, me.organizationId),
        payrollApi.taxYears(activeSettings),
        payrollApi.jurisdictions(activeSettings),
        integrationEnabled
          ? payrollApi.apiClients(activeSettings, me.organizationId)
          : Promise.resolve([]),
        integrationEnabled
          ? payrollApi.webhooks(activeSettings, me.organizationId)
          : Promise.resolve([]),
        integrationEnabled
          ? payrollApi.webhookDeliveries(activeSettings, me.organizationId)
          : Promise.resolve([]),
        payrollApi.filingWorkflows(activeSettings, me.organizationId),
      ])

      const preferredTaxYear =
        taxYearData.find((profile) => profile.taxYear === filingForm.taxYear)?.taxYear ??
        taxYearData[0]?.taxYear ??
        new Date().getFullYear()

      const [filingData, reportData] = await Promise.all([
        payrollApi.filings(activeSettings, me.organizationId, preferredTaxYear),
        payrollApi.payrollSummaryReport(activeSettings, me.organizationId, preferredTaxYear),
      ])

      setCurrentUser(me)
      setDashboard(dashboardData)
      setOrganization(organizationData)
      setEmployees(employeeData)
      setSchedules(scheduleData)
      setRuns(runData)
      setTaxYears(taxYearData)
      setJurisdictions(jurisdictionData)
      setFilings(filingData)
      setReport(reportData)
      setApiClients(apiClientData)
      setWebhooks(webhookData)
      setDeliveries(deliveryData)
      setWorkflows(workflowData)
      setCalculatorForm((previous) => ({
        ...previous,
        employeeId: previous.employeeId || employeeData[0]?.id || 0,
        frequency: scheduleData[0]?.frequency ?? previous.frequency,
        taxYear: preferredTaxYear,
      }))
      setEmployeeDraft(createEmployeeDraft(me.organizationId, preferredTaxYear))
      setApiClientDraft(createApiClientDraft(me.organizationId))
      setWebhookDraft(createWebhookDraft(me.organizationId))
      setFilingForm((previous) => ({
        ...previous,
        organizationId: me.organizationId,
        taxYear: preferredTaxYear,
        filingJurisdictionCode:
          previous.filingJurisdictionCode ??
          organizationData.primaryJurisdictionCode ??
          null,
      }))
      setWorkflowDraft(createWorkflowDraft(me.organizationId, preferredTaxYear))
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

  async function refreshTaxAndReportingViews(
    activeSettings: ConnectionSettings = settings,
    organizationId = currentUser?.organizationId,
    taxYear = filingForm.taxYear,
  ) {
    if (!organizationId) {
      return
    }

    const integrationEnabled = hasRole(currentUser?.roles, [
      'ADMIN',
      'DEVELOPER_PLATFORM_INTEGRATOR',
    ])

    const [filingData, reportData, workflowData, deliveryData, webhookData, apiClientData] =
      await Promise.all([
        payrollApi.filings(activeSettings, organizationId, taxYear),
        payrollApi.payrollSummaryReport(activeSettings, organizationId, taxYear),
        payrollApi.filingWorkflows(activeSettings, organizationId),
        integrationEnabled
          ? payrollApi.webhookDeliveries(activeSettings, organizationId)
          : Promise.resolve([]),
        integrationEnabled
          ? payrollApi.webhooks(activeSettings, organizationId)
          : Promise.resolve([]),
        integrationEnabled
          ? payrollApi.apiClients(activeSettings, organizationId)
          : Promise.resolve([]),
      ])

    setFilings(filingData)
    setReport(reportData)
    setWorkflows(workflowData)
    setDeliveries(deliveryData)
    setWebhooks(webhookData)
    setApiClients(apiClientData)
  }

  useEffect(() => {
    void loadWorkspace(DEFAULT_SETTINGS)
    // Initial load uses the seeded basic-auth admin credentials.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!currentUser) {
      return
    }

    void refreshTaxAndReportingViews(settings, currentUser.organizationId, filingForm.taxYear).catch(
      (unknownError) => {
        setError(
          unknownError instanceof Error
            ? unknownError.message
            : 'Unable to refresh tax and reporting views.',
        )
      },
    )
  }, [currentUser, filingForm.taxYear])

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
      await refreshTaxAndReportingViews()
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

  async function handleStartWorkflow() {
    setStartingWorkflow(true)
    setError(null)
    setNotice(null)

    try {
      await payrollApi.startFilingWorkflow(settings, workflowDraft)
      setNotice('Tax filing workflow started.')
      await refreshTaxAndReportingViews()
    } catch (unknownError) {
      setError(
        unknownError instanceof Error
          ? unknownError.message
          : 'Unable to start the filing workflow.',
      )
    } finally {
      setStartingWorkflow(false)
    }
  }

  async function handleCreateApiClient() {
    setCreatingApiClient(true)
    setError(null)
    setNotice(null)

    try {
      const createdClient = await payrollApi.createApiClient(settings, apiClientDraft)
      setNotice(
        createdClient.issuedApiKey
          ? `Partner API client created. Issued key: ${createdClient.issuedApiKey}`
          : 'Partner API client created.',
      )
      await refreshTaxAndReportingViews()
      if (currentUser) {
        setApiClientDraft(createApiClientDraft(currentUser.organizationId))
      }
    } catch (unknownError) {
      setError(
        unknownError instanceof Error
          ? unknownError.message
          : 'Unable to create the partner API client.',
      )
    } finally {
      setCreatingApiClient(false)
    }
  }

  async function handleCreateWebhook() {
    setCreatingWebhook(true)
    setError(null)
    setNotice(null)

    try {
      const createdWebhook = await payrollApi.createWebhook(settings, webhookDraft)
      setNotice(
        createdWebhook.issuedSigningSecret
          ? `Webhook created. Signing secret: ${createdWebhook.issuedSigningSecret}`
          : 'Webhook created.',
      )
      await refreshTaxAndReportingViews()
      if (currentUser) {
        setWebhookDraft(createWebhookDraft(currentUser.organizationId))
      }
    } catch (unknownError) {
      setError(
        unknownError instanceof Error
          ? unknownError.message
          : 'Unable to create the webhook endpoint.',
      )
    } finally {
      setCreatingWebhook(false)
    }
  }

  async function handleDispatchWebhooks() {
    setDispatchingWebhooks(true)
    setError(null)
    setNotice(null)

    try {
      const result = await payrollApi.dispatchWebhookDeliveries(settings)
      setNotice(`Webhook dispatch processed ${result.processedCount} delivery item(s).`)
      await refreshTaxAndReportingViews()
    } catch (unknownError) {
      setError(
        unknownError instanceof Error
          ? unknownError.message
          : 'Unable to dispatch webhook deliveries.',
      )
    } finally {
      setDispatchingWebhooks(false)
    }
  }

  return (
    <div className="app-shell">
      <header className="hero-shell">
        <div>
          <p className="eyebrow">Embedded Payroll Platform</p>
          <h1>Multi-tenant US payroll, tax, partner APIs, and embedded operations</h1>
          <p className="hero-copy">
            Spring Boot APIs and React UI components for company jurisdictions,
            W-2 gross-to-net payroll, 50-state and local tax support, partner API
            keys, webhooks, idempotent integration writes, and Temporal-backed tax
            filing workflows.
          </p>
        </div>
        <div className="hero-panel">
          <p className="eyebrow">Current actor</p>
          {currentUser ? (
            <>
              <strong>{currentUser.fullName}</strong>
              <span>{currentUser.email ?? currentUser.username}</span>
              <span className="muted">
                {titleCase(currentUser.authenticationMode)}
                {currentUser.partnerClientCode
                  ? ` • ${currentUser.partnerClientCode}`
                  : ''}
              </span>
              <div className="badge-row">
                {currentUser.roles.map((role) => (
                  <RoleBadge key={role} role={role} />
                ))}
              </div>
            </>
          ) : (
            <span className="muted">Connect to the backend to load the tenant.</span>
          )}
        </div>
      </header>

      <SectionCard
        title="Integration connection"
        subtitle="Connect with basic credentials or an API key for partner-programmatic access."
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
          label="Primary jurisdiction"
          value={dashboard?.primaryJurisdictionCode ?? 'n/a'}
          hint={`${dashboard?.jurisdictionRegistrations ?? 0} registrations configured`}
        />
        <StatCard
          label="Partner integrations"
          value={dashboard?.partnerApiClients ?? 0}
          hint={`${dashboard?.pendingWebhookDeliveries ?? 0} pending webhook deliveries`}
        />
        <StatCard
          label="Next pay date"
          value={dashboard ? formatDate(dashboard.nextPayDate) : 'Not scheduled'}
          hint={`${dashboard?.pendingApprovalRuns ?? 0} payroll runs awaiting approval`}
        />
      </div>

      <div className="page-grid">
        <SectionCard
          title="Company and jurisdiction registrations"
          subtitle="Company tenant profile, payroll nexus registrations, and local/state setup used by the multi-tenant engine."
        >
          {organization ? (
            <>
              <div className="result-grid">
                <div className="result-tile">
                  <span>Tenant key</span>
                  <strong>{organization.tenantKey}</strong>
                </div>
                <div className="result-tile">
                  <span>Primary state</span>
                  <strong>{organization.primaryJurisdictionCode}</strong>
                </div>
                <div className="result-tile">
                  <span>HQ local</span>
                  <strong>{organization.headquartersLocalJurisdictionCode ?? 'n/a'}</strong>
                </div>
              </div>
              <div className="table-shell">
                <table>
                  <thead>
                    <tr>
                      <th>Jurisdiction</th>
                      <th>Type</th>
                      <th>Nexus</th>
                      <th>Registration</th>
                    </tr>
                  </thead>
                  <tbody>
                    {organization.jurisdictions.map((jurisdiction) => (
                      <tr key={jurisdiction.id}>
                        <td>
                          <strong>{jurisdiction.jurisdictionName}</strong>
                          <span className="table-meta">{jurisdiction.jurisdictionCode}</span>
                        </td>
                        <td>{titleCase(jurisdiction.jurisdictionType)}</td>
                        <td>{titleCase(jurisdiction.nexusType)}</td>
                        <td>
                          {jurisdiction.registrationNumber ?? 'n/a'}
                          <span className="table-meta">
                            {jurisdiction.employerAccountNumber ?? 'n/a'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          ) : (
            <p className="empty-state">Connect to load company jurisdictions.</p>
          )}
        </SectionCard>

        <SectionCard
          title="Payroll reporting"
          subtitle="Employer reporting across federal, state, local, and employer-side taxes."
        >
          {report ? (
            <>
              <div className="result-grid">
                <div className="result-tile">
                  <span>Gross</span>
                  <strong>{formatCurrency(report.grossTotal)}</strong>
                </div>
                <div className="result-tile">
                  <span>Federal taxes</span>
                  <strong>{formatCurrency(report.federalTaxTotal)}</strong>
                </div>
                <div className="result-tile">
                  <span>State + local</span>
                  <strong>
                    {formatCurrency(report.stateTaxTotal + report.localTaxTotal)}
                  </strong>
                </div>
              </div>
              <div className="table-shell">
                <table>
                  <thead>
                    <tr>
                      <th>State</th>
                      <th>Gross</th>
                      <th>Employee tax</th>
                      <th>Employer tax</th>
                      <th>Net</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.stateSummaries.map((summary) => (
                      <tr key={summary.jurisdictionCode}>
                        <td>{summary.jurisdictionCode}</td>
                        <td>{formatCurrency(summary.grossTotal)}</td>
                        <td>{formatCurrency(summary.employeeTaxTotal)}</td>
                        <td>{formatCurrency(summary.employerTaxTotal)}</td>
                        <td>{formatCurrency(summary.netTotal)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          ) : (
            <p className="empty-state">Reporting becomes available after connection.</p>
          )}
        </SectionCard>
      </div>

      <div className="page-grid">
        <SectionCard
          title="Employee master data and W-4 maintenance"
          subtitle="Maintains employee profile, worker classification, state/local sourcing, and federal W-4 elections."
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
                      setEmployeeDraft({ ...employeeDraft, email: event.target.value })
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
                        compensationType:
                          event.target.value as CreateEmployeeRequest['compensationType'],
                      })
                    }
                  >
                    <option value="SALARIED">Salaried</option>
                    <option value="HOURLY">Hourly</option>
                  </select>
                </label>
                <label>
                  Work state
                  <input
                    value={employeeDraft.workState}
                    onChange={(event) =>
                      setEmployeeDraft({ ...employeeDraft, workState: event.target.value })
                    }
                  />
                </label>
                <label>
                  Residence state
                  <input
                    value={employeeDraft.residenceState}
                    onChange={(event) =>
                      setEmployeeDraft({
                        ...employeeDraft,
                        residenceState: event.target.value,
                      })
                    }
                  />
                </label>
                <label>
                  Work local jurisdiction
                  <input
                    value={employeeDraft.workLocalJurisdictionCode ?? ''}
                    onChange={(event) =>
                      setEmployeeDraft({
                        ...employeeDraft,
                        workLocalJurisdictionCode: event.target.value || null,
                      })
                    }
                  />
                </label>
                <label>
                  Residence local jurisdiction
                  <input
                    value={employeeDraft.residenceLocalJurisdictionCode ?? ''}
                    onChange={(event) =>
                      setEmployeeDraft({
                        ...employeeDraft,
                        residenceLocalJurisdictionCode: event.target.value || null,
                      })
                    }
                  />
                </label>
                <label>
                  Annual salary
                  <input
                    type="number"
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
              </div>
              <button
                type="button"
                onClick={() => void handleCreateEmployee()}
                disabled={submittingEmployee}
              >
                {submittingEmployee ? 'Saving...' : 'Create W-2 employee'}
              </button>
            </div>
          ) : (
            <p className="muted">Current role is read-only for employee maintenance.</p>
          )}
          <EmployeeTable employees={employees} />
        </SectionCard>

        <SectionCard
          title="Gross-to-net calculator"
          subtitle="Uses W-4 elections, FICA/FUTA/FIT, seeded state rules for all 50 states, and local jurisdiction profiles."
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
          subtitle="Multi-model payroll writes remain atomic while runs, line items, and webhook events are persisted."
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
          title="Tax coverage and jurisdiction catalogue"
          subtitle="Federal rules plus seeded state and local jurisdiction records drive the versioned tax engine."
        >
          <div className="tax-year-selector">
            <label>
              Tax year
              <select
                value={filingForm.taxYear}
                onChange={(event) => {
                  const nextTaxYear = Number(event.target.value)
                  setFilingForm({ ...filingForm, taxYear: nextTaxYear })
                  if (currentUser) {
                    setWorkflowDraft(createWorkflowDraft(currentUser.organizationId, nextTaxYear))
                  }
                }}
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
          <p className="muted">
            Loaded jurisdictions: {jurisdictions.length} total records across federal,
            state, and local scopes.
          </p>
        </SectionCard>
      </div>

      <SectionCard
        title="Payroll runs and approval queue"
        subtitle="Approvers release runs after review; partner-integrator actors can monitor the versioned payroll API surface."
      >
        <PayrollRunTable
          runs={runs}
          canApprove={canApprove}
          approvingRunId={approvingRunId}
          onApprove={(runId) => void handleApproveRun(runId)}
        />
      </SectionCard>

      <div className="page-grid">
        <SectionCard
          title="Tax filing records"
          subtitle="Generate jurisdiction-aware filing summaries and start Temporal-compatible filing workflows."
        >
          <TaxFilingTable
            filings={filings}
            form={filingForm}
            onChange={setFilingForm}
            onGenerate={() => void handleGenerateFiling()}
            generating={generatingFiling}
            canGenerate={canEdit}
          />
          {canEdit ? (
            <div className="form-stack">
              <div className="field-grid">
                <label>
                  Workflow filing period
                  <select
                    value={workflowDraft.filingPeriod}
                    onChange={(event) =>
                      setWorkflowDraft({
                        ...workflowDraft,
                        filingPeriod: event.target.value,
                      })
                    }
                  >
                    {['Q1', 'Q2', 'Q3', 'Q4', 'ANNUAL'].map((period) => (
                      <option key={period} value={period}>
                        {period}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <button
                type="button"
                onClick={() => void handleStartWorkflow()}
                disabled={startingWorkflow}
              >
                {startingWorkflow ? 'Starting workflow...' : 'Start filing workflow'}
              </button>
              <div className="table-shell">
                <table>
                  <thead>
                    <tr>
                      <th>Workflow</th>
                      <th>Status</th>
                      <th>Requested</th>
                    </tr>
                  </thead>
                  <tbody>
                    {workflows.map((workflow) => (
                      <tr key={workflow.id}>
                        <td>
                          <strong>{workflow.workflowId}</strong>
                          <span className="table-meta">
                            {workflow.requestedFilings}
                          </span>
                        </td>
                        <td>{titleCase(workflow.workflowStatus)}</td>
                        <td>
                          {workflow.requestedBy}
                          <span className="table-meta">
                            {formatDate(workflow.requestedAt)}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ) : null}
        </SectionCard>

        <SectionCard
          title="Partner API keys, webhooks, and integration operations"
          subtitle="Developer / Platform Integrator users can issue API keys, configure webhook endpoints, and monitor outbound delivery attempts."
        >
          {canManageIntegrations ? (
            <div className="form-stack">
              <div className="field-grid">
                <label>
                  Client code
                  <input
                    value={apiClientDraft.clientCode}
                    onChange={(event) =>
                      setApiClientDraft({
                        ...apiClientDraft,
                        clientCode: event.target.value,
                      })
                    }
                  />
                </label>
                <label>
                  Display name
                  <input
                    value={apiClientDraft.displayName}
                    onChange={(event) =>
                      setApiClientDraft({
                        ...apiClientDraft,
                        displayName: event.target.value,
                      })
                    }
                  />
                </label>
                <label>
                  Scopes
                  <input
                    value={apiClientDraft.scopes}
                    onChange={(event) =>
                      setApiClientDraft({
                        ...apiClientDraft,
                        scopes: event.target.value,
                      })
                    }
                  />
                </label>
              </div>
              <button
                type="button"
                onClick={() => void handleCreateApiClient()}
                disabled={creatingApiClient}
              >
                {creatingApiClient ? 'Issuing key...' : 'Create partner API client'}
              </button>

              <div className="table-shell">
                <table>
                  <thead>
                    <tr>
                      <th>Client</th>
                      <th>Prefix</th>
                      <th>Scopes</th>
                    </tr>
                  </thead>
                  <tbody>
                    {apiClients.map((client) => (
                      <tr key={client.id}>
                        <td>
                          <strong>{client.displayName}</strong>
                          <span className="table-meta">{client.clientCode}</span>
                        </td>
                        <td>{client.keyPrefix}</td>
                        <td>{client.scopes}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="field-grid">
                <label>
                  Partner client
                  <select
                    value={webhookDraft.partnerApiClientId ?? ''}
                    onChange={(event) =>
                      setWebhookDraft({
                        ...webhookDraft,
                        partnerApiClientId: event.target.value
                          ? Number(event.target.value)
                          : null,
                      })
                    }
                  >
                    <option value="">Organization-wide</option>
                    {apiClients.map((client) => (
                      <option key={client.id} value={client.id}>
                        {client.displayName}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Webhook name
                  <input
                    value={webhookDraft.name}
                    onChange={(event) =>
                      setWebhookDraft({ ...webhookDraft, name: event.target.value })
                    }
                  />
                </label>
                <label>
                  Target URL
                  <input
                    value={webhookDraft.targetUrl}
                    onChange={(event) =>
                      setWebhookDraft({
                        ...webhookDraft,
                        targetUrl: event.target.value,
                      })
                    }
                  />
                </label>
                <label>
                  Event
                  <input
                    value={webhookDraft.subscribedEvent}
                    onChange={(event) =>
                      setWebhookDraft({
                        ...webhookDraft,
                        subscribedEvent: event.target.value,
                      })
                    }
                  />
                </label>
              </div>
              <button
                type="button"
                onClick={() => void handleCreateWebhook()}
                disabled={creatingWebhook}
              >
                {creatingWebhook ? 'Creating webhook...' : 'Create webhook'}
              </button>
              <button
                type="button"
                className="button-secondary"
                onClick={() => void handleDispatchWebhooks()}
                disabled={dispatchingWebhooks}
              >
                {dispatchingWebhooks ? 'Dispatching...' : 'Dispatch pending webhooks'}
              </button>

              <div className="table-shell">
                <table>
                  <thead>
                    <tr>
                      <th>Endpoint</th>
                      <th>Event</th>
                      <th>Deliveries</th>
                    </tr>
                  </thead>
                  <tbody>
                    {webhooks.map((webhook) => (
                      <tr key={webhook.id}>
                        <td>
                          <strong>{webhook.name}</strong>
                          <span className="table-meta">{webhook.targetUrl}</span>
                        </td>
                        <td>{webhook.subscribedEvent}</td>
                        <td>
                          {deliveries.filter(
                            (delivery) => delivery.webhookEndpointId === webhook.id,
                          ).length}{' '}
                          events
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ) : (
            <p className="muted">
              Current role cannot manage partner API clients or webhooks.
            </p>
          )}
        </SectionCard>
      </div>
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
