import type {
  FilingType,
  GenerateFilingRequest,
  TaxFilingRecord,
} from '../api/types'
import { formatCurrency, formatDate, titleCase } from '../lib/formatters'

interface TaxFilingTableProps {
  filings: TaxFilingRecord[]
  form: GenerateFilingRequest
  onChange: (form: GenerateFilingRequest) => void
  onGenerate: () => void
  generating: boolean
  canGenerate: boolean
}

const filingTypes: FilingType[] = [
  'FORM_941',
  'FORM_940',
  'FORM_W2',
  'FORM_W3',
  'STATE_WITHHOLDING',
  'LOCAL_WITHHOLDING',
]

export function TaxFilingTable({
  filings,
  form,
  onChange,
  onGenerate,
  generating,
  canGenerate,
}: TaxFilingTableProps) {
  return (
    <div className="filing-layout">
      <div className="field-grid">
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
          Filing type
          <select
            value={form.filingType}
            onChange={(event) =>
              onChange({
                ...form,
                filingType: event.target.value as FilingType,
              })
            }
          >
            {filingTypes.map((filingType) => (
              <option key={filingType} value={filingType}>
                {titleCase(filingType)}
              </option>
            ))}
          </select>
        </label>
        <label>
          Filing period
          <select
            value={form.filingPeriod}
            onChange={(event) =>
              onChange({ ...form, filingPeriod: event.target.value })
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
      {canGenerate ? (
        <button type="button" onClick={onGenerate} disabled={generating}>
          {generating ? 'Generating...' : 'Generate filing record'}
        </button>
      ) : null}

      <div className="table-shell">
        <table>
          <thead>
            <tr>
              <th>Filing</th>
              <th>Due Date</th>
              <th>Status</th>
              <th>Wages</th>
              <th>Tax</th>
            </tr>
          </thead>
          <tbody>
            {filings.map((record) => (
              <tr key={record.id}>
                <td>
                  <strong>{titleCase(record.filingType)}</strong>
                  <span className="table-meta">
                    {record.filingPeriod} • Ref {record.referenceNumber ?? 'pending'}
                  </span>
                </td>
                <td>{formatDate(record.dueDate)}</td>
                <td>{titleCase(record.status)}</td>
                <td>{formatCurrency(record.totalWages)}</td>
                <td>{formatCurrency(record.totalTax)}</td>
              </tr>
            ))}
            {filings.length === 0 ? (
              <tr>
                <td colSpan={5} className="empty-state">
                  No filing records yet for the selected tax year.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </div>
  )
}
