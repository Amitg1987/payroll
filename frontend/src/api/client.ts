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
  TaxFilingRecord,
  TaxYearProfile,
} from './types'

function authHeader({ username, password }: ConnectionSettings): string {
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
      Authorization: authHeader(settings),
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
    request<CurrentUser>(settings, '/api/security/me'),

  dashboard: (settings: ConnectionSettings, organizationId: number) =>
    request<DashboardSummary>(
      settings,
      `/api/dashboard/summary?organizationId=${organizationId}`,
    ),

  employees: (settings: ConnectionSettings, organizationId: number) =>
    request<Employee[]>(settings, `/api/employees?organizationId=${organizationId}`),

  createEmployee: (settings: ConnectionSettings, payload: CreateEmployeeRequest) =>
    request<Employee>(settings, '/api/employees', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  schedules: (settings: ConnectionSettings, organizationId: number) =>
    request<PayrollSchedule[]>(settings, `/api/schedules?organizationId=${organizationId}`),

  processSchedule: (
    settings: ConnectionSettings,
    scheduleId: number,
    payload: Record<string, unknown> = {},
  ) =>
    request<PayrollRun>(settings, `/api/schedules/${scheduleId}/process`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  payrollRuns: (settings: ConnectionSettings, organizationId: number) =>
    request<PayrollRun[]>(settings, `/api/payroll-runs?organizationId=${organizationId}`),

  approvePayrollRun: (settings: ConnectionSettings, runId: number) =>
    request<PayrollRun>(settings, `/api/payroll-runs/${runId}/approve`, {
      method: 'POST',
    }),

  calculatePayroll: (
    settings: ConnectionSettings,
    payload: PayrollCalculationRequest,
  ) =>
    request<PayrollCalculationResult>(settings, '/api/payroll/calculate', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  taxYears: (settings: ConnectionSettings) =>
    request<TaxYearProfile[]>(settings, '/api/tax/years'),

  filings: (settings: ConnectionSettings, organizationId: number, taxYear: number) =>
    request<TaxFilingRecord[]>(
      settings,
      `/api/tax/filings?organizationId=${organizationId}&taxYear=${taxYear}`,
    ),

  generateFiling: (settings: ConnectionSettings, payload: GenerateFilingRequest) =>
    request<TaxFilingRecord>(settings, '/api/tax/filings/generate', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
}
