import type { TaxYearProfile } from '../api/types'
import { formatCurrency, formatPercent, titleCase } from '../lib/formatters'

interface TaxYearCardProps {
  profile: TaxYearProfile | null
}

export function TaxYearCard({ profile }: TaxYearCardProps) {
  if (!profile) {
    return <p className="empty-state">Select a supported tax year to inspect the rule set.</p>
  }

  return (
    <div className="tax-year-grid">
      <div className="tax-year-summary">
        <p className="eyebrow">Tax year {profile.taxYear}</p>
        <h3>IRS/FICA reference profile</h3>
        <p className="muted">{profile.notes}</p>
        <ul className="metric-list">
          <li>
            <span>Social Security wage base</span>
            <strong>{formatCurrency(profile.socialSecurityWageBase)}</strong>
          </li>
          <li>
            <span>Employee Social Security rate</span>
            <strong>{formatPercent(profile.socialSecurityEmployeeRate)}</strong>
          </li>
          <li>
            <span>Employee Medicare rate</span>
            <strong>{formatPercent(profile.medicareEmployeeRate)}</strong>
          </li>
          <li>
            <span>Additional Medicare threshold</span>
            <strong>{formatCurrency(profile.additionalMedicareThreshold)}</strong>
          </li>
        </ul>
      </div>

      <div className="tax-year-summary">
        <h3>Standard deductions</h3>
        <ul className="metric-list">
          <li>
            <span>Single</span>
            <strong>{formatCurrency(profile.standardDeductionSingle)}</strong>
          </li>
          <li>
            <span>Married filing jointly</span>
            <strong>{formatCurrency(profile.standardDeductionMarriedJointly)}</strong>
          </li>
          <li>
            <span>Head of household</span>
            <strong>{formatCurrency(profile.standardDeductionHeadOfHousehold)}</strong>
          </li>
          <li>
            <span>FUTA wage base</span>
            <strong>{formatCurrency(profile.federalUnemploymentWageBase)}</strong>
          </li>
        </ul>
      </div>

      <div className="table-shell tax-year-grid__table">
        <table>
          <thead>
            <tr>
              <th>Filing status</th>
              <th>Bracket</th>
              <th>Lower bound</th>
              <th>Upper bound</th>
              <th>Rate</th>
            </tr>
          </thead>
          <tbody>
            {profile.federalTaxBrackets.map((bracket) => (
              <tr key={`${bracket.filingStatus}-${bracket.bracketOrder}`}>
                <td>{titleCase(bracket.filingStatus)}</td>
                <td>{bracket.bracketOrder}</td>
                <td>{formatCurrency(bracket.lowerBound)}</td>
                <td>
                  {bracket.upperBound === null
                    ? 'No ceiling'
                    : formatCurrency(bracket.upperBound)}
                </td>
                <td>{formatPercent(bracket.rate)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
