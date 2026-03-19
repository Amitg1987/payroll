import type { PayrollRun } from '../api/types'
import { formatCurrency, formatDate, titleCase } from '../lib/formatters'

interface PayrollRunTableProps {
  runs: PayrollRun[]
  canApprove: boolean
  approvingRunId: number | null
  onApprove: (runId: number) => void
}

export function PayrollRunTable({
  runs,
  canApprove,
  approvingRunId,
  onApprove,
}: PayrollRunTableProps) {
  return (
    <div className="run-list">
      {runs.map((run) => (
        <article key={run.id} className="run-card">
          <header className="run-card__header">
            <div>
              <h3>{run.scheduleName}</h3>
              <p className="muted">
                {formatDate(run.periodStart)} - {formatDate(run.periodEnd)} • Pay date{' '}
                {formatDate(run.payDate)}
              </p>
            </div>
            <div className="run-card__status">
              <span className={`pill pill--${run.status.toLowerCase()}`}>
                {titleCase(run.status)}
              </span>
              {canApprove && run.status === 'PENDING_APPROVAL' ? (
                <button
                  type="button"
                  className="button-secondary"
                  onClick={() => onApprove(run.id)}
                  disabled={approvingRunId === run.id}
                >
                  {approvingRunId === run.id ? 'Approving...' : 'Approve'}
                </button>
              ) : null}
            </div>
          </header>

          <div className="run-card__totals">
            <div>
              <span>Gross</span>
              <strong>{formatCurrency(run.grossTotal)}</strong>
            </div>
            <div>
              <span>Taxes</span>
              <strong>{formatCurrency(run.taxTotal)}</strong>
            </div>
            <div>
              <span>Deductions</span>
              <strong>{formatCurrency(run.deductionTotal)}</strong>
            </div>
            <div>
              <span>Net</span>
              <strong>{formatCurrency(run.netTotal)}</strong>
            </div>
          </div>

          <div className="table-shell">
            <table>
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Gross</th>
                  <th>Taxes</th>
                  <th>Net</th>
                </tr>
              </thead>
              <tbody>
                {run.items.map((item) => (
                  <tr key={`${run.id}-${item.employeeId}`}>
                    <td>
                      <strong>{item.employeeName}</strong>
                      <span className="table-meta">{item.employeeNumber}</span>
                    </td>
                    <td>{formatCurrency(item.grossPay)}</td>
                    <td>{formatCurrency(item.employeeTaxTotal)}</td>
                    <td>{formatCurrency(item.netPay)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </article>
      ))}
      {runs.length === 0 ? <p className="empty-state">No payroll runs yet.</p> : null}
    </div>
  )
}
