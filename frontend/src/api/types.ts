export type RoleName = 'ADMIN' | 'ACCOUNTANT' | 'APPROVER'

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
export type FilingRecordStatus = 'GENERATED' | 'SUBMITTED' | 'ACCEPTED' | 'REJECTED'

export interface ConnectionSettings {
  baseUrl: string
  username: string
  password: string
}

export interface CurrentUser {
  id: number
  organizationId: number
  username: string
  fullName: string
  email: string
  roles: RoleName[]
}

export interface DashboardSummary {
  organizationId: number
  organizationName: string
  ein: string
  employeeCount: number
  pendingApprovalRuns: number
  pendingFilings: number
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
  annualSalary: number | null
  hourlyRate: number | null
  standardHoursPerPeriod: number
  stateWithholdingRate: number
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
  employeeTaxTotal: number
  employerSocialSecurityTax: number
  employerMedicareTax: number
  employerFutaTax: number
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
  employeeTaxTotal: number
  employerSocialSecurityTax: number
  employerMedicareTax: number
  employerFutaTax: number
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
  dueDate: string
  status: FilingRecordStatus
  totalWages: number
  totalTax: number
  generatedAt: string
  submittedAt: string | null
  referenceNumber: string | null
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
}

export interface GenerateFilingRequest {
  organizationId: number
  taxYear: number
  filingType: FilingType
  filingPeriod: string
  dueDate?: string
}
