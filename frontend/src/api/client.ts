import type {
  CreateApiClientRequest,
  ConnectionSettings,
  CreateEmployeeRequest,
  CreateWebhookRequest,
  CurrentUser,
  DashboardSummary,
  Employee,
  FilingWorkflowRequest,
  GenerateFilingRequest,
  JurisdictionTaxProfile,
  PartnerApiClient,
  PayrollCalculationRequest,
  PayrollCalculationResult,
  PayrollSummaryReport,
  PayrollRun,
  PayrollSchedule,
  StartFilingWorkflowRequest,
  TaxFilingRecord,
  TaxJurisdiction,
  TaxYearProfile,
  WebhookDelivery,
  WebhookEndpoint,
  OrganizationProfile,
} from './types'

const API_BASE_PATH = '/api/v1'

function basicAuthHeader({ username, password }: ConnectionSettings): string {
  return `Basic ${btoa(`${username}:${password}`)}`
}

async function request<T>(
  settings: ConnectionSettings,
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const response = await fetch(`${settings.baseUrl}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(settings.apiKey
        ? { 'X-API-Key': settings.apiKey }
        : { Authorization: basicAuthHeader(settings) }),
      ...(init.headers ?? {}),
    },
  })

  if (!response.ok) {
    const fallbackMessage = `Request failed with status ${response.status}`
    try {
      const errorBody = (await response.json()) as { message?: string }
      throw new Error(errorBody.message ?? fallbackMessage)
    } catch {
      throw new Error(fallbackMessage)
    }
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export const payrollApi = {
  currentUser: (settings: ConnectionSettings) =>
    request<CurrentUser>(settings, `${API_BASE_PATH}/security/me`),

  currentOrganization: (settings: ConnectionSettings) =>
    request<OrganizationProfile>(settings, `${API_BASE_PATH}/organizations/current`),

  dashboard: (settings: ConnectionSettings, organizationId: number) =>
    request<DashboardSummary>(
      settings,
      `${API_BASE_PATH}/dashboard/summary?organizationId=${organizationId}`,
    ),

  employees: (settings: ConnectionSettings, organizationId: number) =>
    request<Employee[]>(
      settings,
      `${API_BASE_PATH}/employees?organizationId=${organizationId}`,
    ),

  createEmployee: (settings: ConnectionSettings, payload: CreateEmployeeRequest) =>
    request<Employee>(settings, `${API_BASE_PATH}/employees`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  schedules: (settings: ConnectionSettings, organizationId: number) =>
    request<PayrollSchedule[]>(
      settings,
      `${API_BASE_PATH}/schedules?organizationId=${organizationId}`,
    ),

  processSchedule: (
    settings: ConnectionSettings,
    scheduleId: number,
    payload: Record<string, unknown> = {},
  ) =>
    request<PayrollRun>(
      settings,
      `${API_BASE_PATH}/schedules/${scheduleId}/process`,
      {
      method: 'POST',
      body: JSON.stringify(payload),
      },
    ),

  payrollRuns: (settings: ConnectionSettings, organizationId: number) =>
    request<PayrollRun[]>(
      settings,
      `${API_BASE_PATH}/payroll-runs?organizationId=${organizationId}`,
    ),

  approvePayrollRun: (settings: ConnectionSettings, runId: number) =>
    request<PayrollRun>(settings, `${API_BASE_PATH}/payroll-runs/${runId}/approve`, {
      method: 'POST',
    }),

  calculatePayroll: (
    settings: ConnectionSettings,
    payload: PayrollCalculationRequest,
  ) =>
    request<PayrollCalculationResult>(settings, `${API_BASE_PATH}/payroll/calculate`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  taxYears: (settings: ConnectionSettings) =>
    request<TaxYearProfile[]>(settings, `${API_BASE_PATH}/tax/years`),

  filings: (settings: ConnectionSettings, organizationId: number, taxYear: number) =>
    request<TaxFilingRecord[]>(
      settings,
      `${API_BASE_PATH}/tax/filings?organizationId=${organizationId}&taxYear=${taxYear}`,
    ),

  generateFiling: (settings: ConnectionSettings, payload: GenerateFilingRequest) =>
    request<TaxFilingRecord>(settings, `${API_BASE_PATH}/tax/filings/generate`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  filingWorkflows: (settings: ConnectionSettings, organizationId: number) =>
    request<FilingWorkflowRequest[]>(
      settings,
      `${API_BASE_PATH}/tax/workflows/filings?organizationId=${organizationId}`,
    ),

  startFilingWorkflow: (
    settings: ConnectionSettings,
    payload: StartFilingWorkflowRequest,
  ) =>
    request<FilingWorkflowRequest>(settings, `${API_BASE_PATH}/tax/workflows/filings`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  jurisdictions: (settings: ConnectionSettings) =>
    request<TaxJurisdiction[]>(settings, `${API_BASE_PATH}/tax/jurisdictions`),

  jurisdictionProfiles: (
    settings: ConnectionSettings,
    taxYear: number,
    taxType: string,
  ) =>
    request<JurisdictionTaxProfile[]>(
      settings,
      `${API_BASE_PATH}/tax/jurisdiction-profiles?taxYear=${taxYear}&taxType=${taxType}`,
    ),

  payrollSummaryReport: (
    settings: ConnectionSettings,
    organizationId: number,
    taxYear: number,
  ) =>
    request<PayrollSummaryReport>(
      settings,
      `${API_BASE_PATH}/reports/payroll-summary?organizationId=${organizationId}&taxYear=${taxYear}`,
    ),

  apiClients: (settings: ConnectionSettings, organizationId: number) =>
    request<PartnerApiClient[]>(
      settings,
      `${API_BASE_PATH}/integration/api-clients?organizationId=${organizationId}`,
    ),

  createApiClient: (settings: ConnectionSettings, payload: CreateApiClientRequest) =>
    request<PartnerApiClient>(settings, `${API_BASE_PATH}/integration/api-clients`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  webhooks: (settings: ConnectionSettings, organizationId: number) =>
    request<WebhookEndpoint[]>(
      settings,
      `${API_BASE_PATH}/integration/webhooks?organizationId=${organizationId}`,
    ),

  createWebhook: (settings: ConnectionSettings, payload: CreateWebhookRequest) =>
    request<WebhookEndpoint>(settings, `${API_BASE_PATH}/integration/webhooks`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  webhookDeliveries: (settings: ConnectionSettings, organizationId: number) =>
    request<WebhookDelivery[]>(
      settings,
      `${API_BASE_PATH}/integration/webhook-deliveries?organizationId=${organizationId}`,
    ),

  dispatchWebhookDeliveries: (settings: ConnectionSettings) =>
    request<{ processedCount: number }>(
      settings,
      `${API_BASE_PATH}/integration/webhook-deliveries/dispatch`,
      { method: 'POST' },
    ),
}
