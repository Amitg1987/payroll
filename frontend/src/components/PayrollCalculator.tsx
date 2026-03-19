import type {
  Employee,
  PayrollCalculationRequest,
  PayrollCalculationResult,
  PayrollSchedule,
} from '../api/types'
import { formatCurrency, titleCase } from '../lib/formatters'

interface PayrollCalculatorProps {
  employees: Employee[]
  schedules: PayrollSchedule[]
  form: PayrollCalculationRequest
  onChange: (form: PayrollCalculationRequest) => void
  onSubmit: () => void
  result: PayrollCalculationResult | null
  loading: boolean
}

export function PayrollCalculator({
  employees,
  schedules,
  form,
  onChange,
  onSubmit,
  result,
  loading,
}: PayrollCalculatorProps) {
  return (
    <div className="calculator-grid">
      <div className="field-grid">
        <label>
          Employee
          <select
            value={form.employeeId}
            onChange={(event) =>
              onChange({ ...form, employeeId: Number(event.target.value) })
            }
          >
            {employees.map((employee) => (
              <option key={employee.id} value={employee.id}>
                {employee.firstName} {employee.lastName}
              </option>
            ))}
          </select>
        </label>
        <label>
          Tax year
          <input
            type="number"
            value={form.taxYear}
            onChange={(event) =>
              onChange({ ...form, taxYear: Number(event.target.value) })
            }
          />
        </label>
        <label>
          Frequency
          <select
            value={form.frequency}
            onChange={(event) =>
              onChange({
                ...form,
                frequency: event.target.value as PayrollCalculationRequest['frequency'],
              })
            }
          >
            {Array.from(new Set(schedules.map((schedule) => schedule.frequency))).map(
              (frequency) => (
                <option key={frequency} value={frequency}>
                  {titleCase(frequency)}
                </option>
              ),
            )}
          </select>
        </label>
        <label>
          Bonus pay
          <input
            type="number"
            min="0"
            step="0.01"
            value={form.bonusPay}
            onChange={(event) =>
              onChange({ ...form, bonusPay: Number(event.target.value) })
            }
          />
        </label>
        <label>
          Overtime hours
          <input
            type="number"
            min="0"
            step="0.25"
            value={form.overtimeHours}
            onChange={(event) =>
              onChange({ ...form, overtimeHours: Number(event.target.value) })
            }
          />
        </label>
        <label>
          Pre-tax deductions
          <input
            type="number"
            min="0"
            step="0.01"
            value={form.preTaxDeductions}
            onChange={(event) =>
              onChange({ ...form, preTaxDeductions: Number(event.target.value) })
            }
          />
        </label>
      </div>

      <div className="calculator-actions">
        <button type="button" onClick={onSubmit} disabled={loading || employees.length === 0}>
          {loading ? 'Calculating...' : 'Run gross-to-net'}
        </button>
      </div>

      {result ? (
        <div className="result-grid">
          <div className="result-tile">
            <span>Gross pay</span>
            <strong>{formatCurrency(result.grossPay)}</strong>
          </div>
          <div className="result-tile">
            <span>Employee taxes</span>
            <strong>{formatCurrency(result.employeeTaxTotal)}</strong>
          </div>
          <div className="result-tile result-tile--highlight">
            <span>Net pay</span>
            <strong>{formatCurrency(result.netPay)}</strong>
          </div>
          <div className="result-breakdown">
            <p>
              <strong>Federal withholding:</strong> {formatCurrency(result.federalIncomeTax)}
            </p>
            <p>
              <strong>Social Security:</strong>{' '}
              {formatCurrency(result.socialSecurityEmployeeTax)}
            </p>
            <p>
              <strong>Medicare:</strong> {formatCurrency(result.medicareEmployeeTax)}
            </p>
            <p>
              <strong>Additional Medicare:</strong>{' '}
              {formatCurrency(result.additionalMedicareEmployeeTax)}
            </p>
            <p>
              <strong>State tax:</strong> {formatCurrency(result.stateIncomeTax)}
            </p>
            <p>
              <strong>Employer FUTA:</strong> {formatCurrency(result.employerFutaTax)}
            </p>
          </div>
        </div>
      ) : null}
    </div>
  )
}
