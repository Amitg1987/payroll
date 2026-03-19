export type RoleName =
  | 'ADMIN'
  | 'ACCOUNTANT'
  | 'APPROVER'
  | 'DEVELOPER_PLATFORM_INTEGRATOR'
  | 'PARTNER_API'

export type FilingStatus =
  | 'SINGLE'
  | 'MARRIED_FILING_JOINTLY'
  | 'HEAD_OF_HOUSEHOLD'

export type CompensationType = 'SALARIED' | 'HOURLY'
export type EmploymentStatus = 'ACTIVE' | 'ON_LEAVE' | 'TERMINATED'
export type PayrollFrequency = 'WEEKLY' | 'BIWEEKLY' | 'SEMIMONTHLY' | 'MONTHLY'
export type PayrollRunStatus =
  | 'DRAFT'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'POSTED'
  | 'REJECTED'
export type FilingType =
  | 'FORM_941'
  | 'FORM_940'
  | 'FORM_W2'
  | 'FORM_W3'
  | 'STATE_WITHHOLDING'
  | 'LOCAL_WITHHOLDING'
export type FilingRecordStatus = 'GENERATED' | 'SUBMITTED' | 'ACCEPTED' | 'REJECTED'
export type AuthenticationMode = 'BASIC_USER' | 'API_KEY'
export type WorkerType = 'W2_EMPLOYEE'
export type JurisdictionType = 'FEDERAL' | 'STATE' | 'LOCAL'
export type NexusType = 'REGISTERED' | 'WORK_LOCATION' | 'RESIDENCE' | 'BOTH'
export type TaxType = 'STATE_WITHHOLDING' | 'LOCAL_WITHHOLDING' | 'STATE_UNEMPLOYMENT'
export type WebhookDeliveryStatus = 'PENDING' | 'SENT' | 'FAILED' | 'DEAD'
export type WorkflowStatus =
  | 'QUEUED'
  | 'STARTED'
  | 'COMPLETED'
  | 'FAILED'
  | 'LOCAL_DEV_FALLBACK'

export interface ConnectionSettings {
  baseUrl: string
  username: string
  password: string
  apiKey: string
}

export interface CurrentUser {
  id: number | null
  organizationId: number
  username: string
  fullName: string
  email: string | null
  roles: RoleName[]
  authenticationMode: AuthenticationMode
  partnerClientCode: string | null
}

export interface DashboardSummary {
  organizationId: number
  organizationName: string
  ein: string
  primaryJurisdictionCode: string
  employeeCount: number
  pendingApprovalRuns: number
  pendingFilings: number
  jurisdictionRegistrations: number
  partnerApiClients: number
  pendingWebhookDeliveries: number
  nextPayDate: string | null
  supportedTaxYears: number[]
}

export interface W4Profile {
  id: number
  taxYear: number
  filingStatus: FilingStatus
  multipleJobs: boolean
  dependentsCredit: number
  otherIncome: number
  deductions: number
  extraWithholding: number
  exemptFromWithholding: boolean
}

export interface Employee {
  id: number
  organizationId: number
  employeeNumber: string
  firstName: string
  lastName: string
  email: string
  ssnLastFour: string
  department: string
  workState: string
  hireDate: string
  employmentStatus: EmploymentStatus
  compensationType: CompensationType
  workerType: WorkerType
  annualSalary: number | null
  hourlyRate: number | null
  standardHoursPerPeriod: number
  stateWithholdingRate: number
  residenceState: string
  workLocalJurisdictionCode: string | null
  residenceLocalJurisdictionCode: string | null
  w4Profile: W4Profile | null
}

export interface PayrollSchedule {
  id: number
  organizationId: number
  name: string
  frequency: PayrollFrequency
  nextPayDate: string
  approvalRequired: boolean
  active: boolean
}

export interface PayrollLineItem {
  employeeId: number
  employeeNumber: string
  employeeName: string
  taxYear: number
  grossPay: number
  bonusPay: number
  overtimePay: number
  preTaxDeductions: number
  taxableWages: number
  federalIncomeTax: number
  socialSecurityEmployeeTax: number
  medicareEmployeeTax: number
  additionalMedicareEmployeeTax: number
  stateIncomeTax: number
  workStateIncomeTax: number
  residentStateIncomeTax: number
  residentStateCreditOffset: number
  localIncomeTax: number
  employeeTaxTotal: number
  employerSocialSecurityTax: number
  employerMedicareTax: number
  employerFutaTax: number
  employerStateUnemploymentTax: number
  socialSecurityTaxableWages: number
  federalUnemploymentTaxableWages: number
  stateUnemploymentTaxableWages: number
  stateJurisdictionCode: string | null
  localJurisdictionCode: string | null
  residentStateJurisdictionCode: string | null
  allocations: WorkLocationAllocation[]
  netPay: number
}

export interface PayrollRun {
  id: number
  scheduleId: number
  scheduleName: string
  periodStart: string
  periodEnd: string
  payDate: string
  taxYear: number
  status: PayrollRunStatus
  createdBy: string
  approvedBy: string | null
  grossTotal: number
  taxTotal: number
  deductionTotal: number
  netTotal: number
  items: PayrollLineItem[]
}

export interface PayrollCalculationResult {
  employeeId: number
  employeeName: string
  taxYear: number
  filingStatus: FilingStatus
  grossPay: number
  taxableWages: number
  federalIncomeTax: number
  socialSecurityEmployeeTax: number
  medicareEmployeeTax: number
  additionalMedicareEmployeeTax: number
  stateIncomeTax: number
  workStateIncomeTax: number
  residentStateIncomeTax: number
  residentStateCreditOffset: number
  localIncomeTax: number
  employeeTaxTotal: number
  employerSocialSecurityTax: number
  employerMedicareTax: number
  employerFutaTax: number
  employerStateUnemploymentTax: number
  socialSecurityTaxableWages: number
  federalUnemploymentTaxableWages: number
  stateUnemploymentTaxableWages: number
  residentStateJurisdictionCode: string | null
  allocations: WorkLocationAllocation[]
  netPay: number
}

export interface TaxBracket {
  filingStatus: FilingStatus
  bracketOrder: number
  lowerBound: number
  upperBound: number | null
  rate: number
}

export interface TaxYearProfile {
  taxYear: number
  socialSecurityEmployeeRate: number
  socialSecurityEmployerRate: number
  socialSecurityWageBase: number
  medicareEmployeeRate: number
  medicareEmployerRate: number
  additionalMedicareRate: number
  additionalMedicareThreshold: number
  federalUnemploymentRate: number
  federalUnemploymentWageBase: number
  defaultStateUnemploymentRate: number
  standardDeductionSingle: number
  standardDeductionMarriedJointly: number
  standardDeductionHeadOfHousehold: number
  notes: string
  federalTaxBrackets: TaxBracket[]
}

export interface TaxFilingRecord {
  id: number
  organizationId: number
  filingType: FilingType
  taxYear: number
  filingPeriod: string
  filingJurisdictionCode: string
  dueDate: string
  status: FilingRecordStatus
  totalWages: number
  totalTax: number
  generatedAt: string
  submittedAt: string | null
  referenceNumber: string | null
  temporalWorkflowId: string | null
  temporalRunId: string | null
}

export interface CreateEmployeeRequest {
  organizationId: number
  employeeNumber: string
  firstName: string
  lastName: string
  email: string
  ssnLastFour: string
  hireDate: string
  employmentStatus: EmploymentStatus
  compensationType: CompensationType
  department: string
  workState: string
  residenceState: string
  workLocalJurisdictionCode: string | null
  residenceLocalJurisdictionCode: string | null
  annualSalary: number | null
  hourlyRate: number | null
  standardHoursPerPeriod: number
  stateWithholdingRate: number
  w4Profile: {
    taxYear: number
    filingStatus: FilingStatus
    multipleJobs: boolean
    dependentsCredit: number
    otherIncome: number
    deductions: number
    extraWithholding: number
    exemptFromWithholding: boolean
  }
}

export interface PayrollCalculationRequest {
  employeeId: number
  taxYear: number
  frequency: PayrollFrequency
  bonusPay: number
  overtimeHours: number
  preTaxDeductions: number
  workLocationAllocations?: WorkLocationAllocationRequest[]
}

export interface GenerateFilingRequest {
  organizationId: number
  taxYear: number
  filingType: FilingType
  filingPeriod: string
  filingJurisdictionCode?: string | null
  dueDate?: string
}

export interface WorkLocationAllocationRequest {
  stateJurisdictionCode: string
  localJurisdictionCode?: string | null
  allocationPercentage: number
}

export interface WorkLocationAllocation {
  stateJurisdictionCode: string
  localJurisdictionCode: string | null
  allocationPercentage: number
  allocatedGrossWages: number
  allocatedTaxableWages: number
  workStateIncomeTax: number
  localIncomeTax: number
  stateUnemploymentTaxableWages: number
  employerStateUnemploymentTax: number
  residentStateCreditApplied: number
  reciprocityApplied: boolean
}

export interface OrganizationJurisdiction {
  id: number
  jurisdictionCode: string
  jurisdictionName: string
  jurisdictionType: JurisdictionType
  nexusType: NexusType
  registrationNumber: string | null
  employerAccountNumber: string | null
  primaryJurisdiction: boolean
}

export interface OrganizationProfile {
  id: number
  tenantKey: string
  code: string
  legalName: string
  dbaName: string | null
  ein: string
  countryCode: string
  defaultStateCode: string
  primaryJurisdictionCode: string
  headquartersLocalJurisdictionCode: string | null
  contactEmail: string
  jurisdictions: OrganizationJurisdiction[]
}

export interface PartnerApiClient {
  id: number
  organizationId: number
  clientCode: string
  displayName: string
  keyPrefix: string
  scopes: string
  description: string | null
  active: boolean
  issuedApiKey: string | null
}

export interface CreateApiClientRequest {
  organizationId: number
  clientCode: string
  displayName: string
  scopes: string
  description?: string
}

export interface WebhookEndpoint {
  id: number
  organizationId: number
  partnerApiClientId: number | null
  partnerClientCode: string | null
  name: string
  targetUrl: string
  subscribedEvent: string
  active: boolean
  issuedSigningSecret: string | null
}

export interface CreateWebhookRequest {
  organizationId: number
  partnerApiClientId?: number | null
  name: string
  targetUrl: string
  subscribedEvent: string
}

export interface WebhookDelivery {
  id: number
  webhookEndpointId: number
  endpointName: string
  eventType: string
  eventKey: string
  deliveryStatus: WebhookDeliveryStatus
  attempts: number
  nextAttemptAt: string | null
  deliveredAt: string | null
  responseStatusCode: number | null
  responseBody: string | null
}

export interface PayrollSummaryReport {
  organizationId: number
  taxYear: number
  grossTotal: number
  federalTaxTotal: number
  stateTaxTotal: number
  localTaxTotal: number
  employerTaxTotal: number
  netTotal: number
  stateSummaries: Array<{
    jurisdictionCode: string
    grossTotal: number
    employeeTaxTotal: number
    employerTaxTotal: number
    netTotal: number
  }>
}

export interface TaxJurisdiction {
  code: string
  name: string
  jurisdictionType: JurisdictionType
  countryCode: string
  stateCode: string | null
  parentJurisdictionCode: string | null
  active: boolean
}

export interface JurisdictionTaxProfile {
  jurisdictionCode: string
  jurisdictionName: string
  taxYear: number
  taxType: TaxType
  residentRate: number
  nonResidentRate: number
  employerRate: number
  wageBase: number | null
  standardDeduction: number
  notes: string | null
}

export interface StartFilingWorkflowRequest {
  organizationId: number
  taxYear: number
  filingPeriod: string
  filingTypes: FilingType[]
}

export interface FilingWorkflowRequest {
  id: number
  organizationId: number
  taxYear: number
  filingPeriod: string
  requestedFilings: string
  requestedBy: string
  workflowId: string
  workflowRunId: string | null
  workflowStatus: WorkflowStatus
  requestedAt: string
  workflowMessage: string | null
}
