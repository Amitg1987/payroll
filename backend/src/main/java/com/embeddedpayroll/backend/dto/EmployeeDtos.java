package com.embeddedpayroll.backend.dto;

import com.embeddedpayroll.backend.model.Employee;
import com.embeddedpayroll.backend.model.EmployeeW4Profile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class EmployeeDtos {

    private EmployeeDtos() {
    }

    public static EmployeeResponse fromEntity(Employee employee, EmployeeW4Profile w4Profile) {
        return new EmployeeResponse(
            employee.getId(),
            employee.getOrganization().getId(),
            employee.getEmployeeNumber(),
            employee.getFirstName(),
            employee.getLastName(),
            employee.getEmail(),
            employee.getSsnLastFour(),
            employee.getDepartment(),
            employee.getWorkState(),
            employee.getHireDate(),
            employee.getEmploymentStatus(),
            employee.getCompensationType(),
            employee.getWorkerType(),
            employee.getAnnualSalary(),
            employee.getHourlyRate(),
            employee.getStandardHoursPerPeriod(),
            employee.getStateWithholdingRate(),
            employee.getResidenceState(),
            employee.getWorkLocalJurisdictionCode(),
            employee.getResidenceLocalJurisdictionCode(),
            fromEntity(w4Profile)
        );
    }

    public static W4Response fromEntity(EmployeeW4Profile w4Profile) {
        if (w4Profile == null) {
            return null;
        }
        return new W4Response(
            w4Profile.getId(),
            w4Profile.getTaxYear(),
            w4Profile.getFilingStatus(),
            w4Profile.isMultipleJobs(),
            w4Profile.getDependentsCredit(),
            w4Profile.getOtherIncome(),
            w4Profile.getDeductions(),
            w4Profile.getExtraWithholding(),
            w4Profile.isExemptFromWithholding()
        );
    }

    public record CreateEmployeeRequest(
        @NotNull Long organizationId,
        @NotBlank String employeeNumber,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email @NotBlank String email,
        @NotBlank String ssnLastFour,
        @NotNull LocalDate hireDate,
        @NotNull Employee.EmploymentStatus employmentStatus,
        @NotNull Employee.CompensationType compensationType,
        @NotBlank String department,
        @NotBlank String workState,
        @NotBlank String residenceState,
        String workLocalJurisdictionCode,
        String residenceLocalJurisdictionCode,
        @DecimalMin("0.0") BigDecimal annualSalary,
        @DecimalMin("0.0") BigDecimal hourlyRate,
        @NotNull @DecimalMin("0.0") BigDecimal standardHoursPerPeriod,
        @NotNull @DecimalMin("0.0") BigDecimal stateWithholdingRate,
        @Valid @NotNull W4Request w4Profile
    ) {
    }

    public record W4Request(
        @NotNull Integer taxYear,
        @NotNull EmployeeW4Profile.FilingStatus filingStatus,
        boolean multipleJobs,
        @NotNull @DecimalMin("0.0") BigDecimal dependentsCredit,
        @NotNull @DecimalMin("0.0") BigDecimal otherIncome,
        @NotNull @DecimalMin("0.0") BigDecimal deductions,
        @NotNull @DecimalMin("0.0") BigDecimal extraWithholding,
        boolean exemptFromWithholding
    ) {
    }

    public record EmployeeResponse(
        Long id,
        Long organizationId,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String ssnLastFour,
        String department,
        String workState,
        LocalDate hireDate,
        Employee.EmploymentStatus employmentStatus,
        Employee.CompensationType compensationType,
        Employee.WorkerType workerType,
        BigDecimal annualSalary,
        BigDecimal hourlyRate,
        BigDecimal standardHoursPerPeriod,
        BigDecimal stateWithholdingRate,
        String residenceState,
        String workLocalJurisdictionCode,
        String residenceLocalJurisdictionCode,
        W4Response w4Profile
    ) {
    }

    public record W4Response(
        Long id,
        Integer taxYear,
        EmployeeW4Profile.FilingStatus filingStatus,
        boolean multipleJobs,
        BigDecimal dependentsCredit,
        BigDecimal otherIncome,
        BigDecimal deductions,
        BigDecimal extraWithholding,
        boolean exemptFromWithholding
    ) {
    }
}
