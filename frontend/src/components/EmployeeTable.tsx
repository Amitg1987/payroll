import type { Employee } from '../api/types'
import { formatCurrency, formatDate, formatPercent, titleCase } from '../lib/formatters'

interface EmployeeTableProps {
  employees: Employee[]
}

export function EmployeeTable({ employees }: EmployeeTableProps) {
  return (
    <div className="table-shell">
      <table>
        <thead>
          <tr>
            <th>Employee</th>
            <th>Department</th>
            <th>Compensation</th>
            <th>State</th>
            <th>W-4 Profile</th>
            <th>Hire Date</th>
          </tr>
        </thead>
        <tbody>
          {employees.map((employee) => (
            <tr key={employee.id}>
              <td>
                <strong>
                  {employee.firstName} {employee.lastName}
                </strong>
                <span className="table-meta">
                  {employee.employeeNumber} • ***-**-{employee.ssnLastFour}
                </span>
              </td>
              <td>
                {employee.department}
                <span className="table-meta">{titleCase(employee.employmentStatus)}</span>
              </td>
              <td>
                {employee.compensationType === 'SALARIED'
                  ? formatCurrency(employee.annualSalary)
                  : `${formatCurrency(employee.hourlyRate)} / hour`}
                <span className="table-meta">
                  {formatPercent(employee.stateWithholdingRate)} state withholding
                </span>
              </td>
              <td>{employee.workState}</td>
              <td>
                {employee.w4Profile ? titleCase(employee.w4Profile.filingStatus) : 'Missing W-4'}
                <span className="table-meta">
                  Tax year {employee.w4Profile?.taxYear ?? 'n/a'}
                </span>
              </td>
              <td>{formatDate(employee.hireDate)}</td>
            </tr>
          ))}
          {employees.length === 0 ? (
            <tr>
              <td colSpan={6} className="empty-state">
                No employees found.
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </div>
  )
}
