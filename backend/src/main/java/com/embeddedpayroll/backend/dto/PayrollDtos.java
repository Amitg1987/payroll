package com.embeddedpayroll.backend.dto;

import com.embeddedpayroll.backend.model.EmployeeW4Profile;
import com.embeddedpayroll.backend.model.PayrollRun;
import com.embeddedpayroll.backend.model.PayrollRunItem;
import com.embeddedpayroll.backend.model.PayrollSchedule;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PayrollDtos {

    private PayrollDtos() {
    }

    public static ScheduleResponse fromEntity(PayrollSchedule payrollSchedule) {
        return new ScheduleResponse(
            payrollSchedule.getId(),
            payrollSchedule.getOrganization().getId(),
            payrollSchedule.getName(),
            payrollSchedule.getFrequency(),
            payrollSchedule.getNextPayDate(),
            payrollSchedule.isApprovalRequired(),
            payrollSchedule.isActive()
        );
    }

    public static PayrollRunResponse fromEntity(PayrollRun payrollRun, List<PayrollLineResponse> items) {
        return new PayrollRunResponse(
            payrollRun.getId(),
            payrollRun.getPayrollSchedule().getId(),
            payrollRun.getPayrollSchedule().getName(),
            payrollRun.getPeriodStart(),
            payrollRun.getPeriodEnd(),
            payrollRun.getPayDate(),
            payrollRun.getTaxYear(),
            payrollRun.getStatus(),
            payrollRun.getCreatedBy(),
            payrollRun.getApprovedBy(),
            payrollRun.getGrossTotal(),
            payrollRun.getTaxTotal(),
            payrollRun.getDeductionTotal(),
            payrollRun.getNetTotal(),
            items
        );
    }

    public static PayrollLineResponse fromEntity(PayrollRunItem item) {
        return new PayrollLineResponse(
            item.getEmployee().getId(),
            item.getEmployee().getEmployeeNumber(),
            item.getEmployee().getFirstName() + " " + item.getEmployee().getLastName(),
            item.getTaxYear(),
            item.getGrossPay(),
            item.getBonusPay(),
            item.getOvertimePay(),
            item.getPreTaxDeductions(),
            item.getTaxableWages(),
            item.getFederalIncomeTax(),
            item.getSocialSecurityEmployeeTax(),
            item.getMedicareEmployeeTax(),
            item.getAdditionalMedicareEmployeeTax(),
            item.getStateIncomeTax(),
            item.getEmployeeTaxTotal(),
            item.getEmployerSocialSecurityTax(),
            item.getEmployerMedicareTax(),
            item.getEmployerFutaTax(),
            item.getNetPay()
        );
    }

    public record CreateScheduleRequest(
        @NotNull Long organizationId,
        @NotBlank String name,
        @NotNull PayrollSchedule.PayrollFrequency frequency,
        @NotNull LocalDate nextPayDate,
        boolean approvalRequired
    ) {
    }

    public record ProcessScheduleRequest(
        LocalDate payDate,
        Integer taxYear,
        List<EmployeeAdjustmentRequest> adjustments
    ) {
    }

    public record EmployeeAdjustmentRequest(
        @NotNull Long employeeId,
        @NotNull @DecimalMin("0.0") BigDecimal bonusPay,
        @NotNull @DecimalMin("0.0") BigDecimal overtimeHours,
        @NotNull @DecimalMin("0.0") BigDecimal preTaxDeductions
    ) {
    }

    public record PayrollCalculationRequest(
        @NotNull Long employeeId,
        @NotNull Integer taxYear,
        @NotNull PayrollSchedule.PayrollFrequency frequency,
        @NotNull @DecimalMin("0.0") BigDecimal bonusPay,
        @NotNull @DecimalMin("0.0") BigDecimal overtimeHours,
        @NotNull @DecimalMin("0.0") BigDecimal preTaxDeductions
    ) {
    }

    public record ScheduleResponse(
        Long id,
        Long organizationId,
        String name,
        PayrollSchedule.PayrollFrequency frequency,
        LocalDate nextPayDate,
        boolean approvalRequired,
        boolean active
    ) {
    }

    public record PayrollRunResponse(
        Long id,
        Long scheduleId,
        String scheduleName,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate payDate,
        Integer taxYear,
        PayrollRun.RunStatus status,
        String createdBy,
        String approvedBy,
        BigDecimal grossTotal,
        BigDecimal taxTotal,
        BigDecimal deductionTotal,
        BigDecimal netTotal,
        List<PayrollLineResponse> items
    ) {
    }

    public record PayrollLineResponse(
        Long employeeId,
        String employeeNumber,
        String employeeName,
        Integer taxYear,
        BigDecimal grossPay,
        BigDecimal bonusPay,
        BigDecimal overtimePay,
        BigDecimal preTaxDeductions,
        BigDecimal taxableWages,
        BigDecimal federalIncomeTax,
        BigDecimal socialSecurityEmployeeTax,
        BigDecimal medicareEmployeeTax,
        BigDecimal additionalMedicareEmployeeTax,
        BigDecimal stateIncomeTax,
        BigDecimal employeeTaxTotal,
        BigDecimal employerSocialSecurityTax,
        BigDecimal employerMedicareTax,
        BigDecimal employerFutaTax,
        BigDecimal netPay
    ) {
    }

    public record PayrollCalculationResponse(
        Long employeeId,
        String employeeName,
        Integer taxYear,
        EmployeeW4Profile.FilingStatus filingStatus,
        BigDecimal grossPay,
        BigDecimal taxableWages,
        BigDecimal federalIncomeTax,
        BigDecimal socialSecurityEmployeeTax,
        BigDecimal medicareEmployeeTax,
        BigDecimal additionalMedicareEmployeeTax,
        BigDecimal stateIncomeTax,
        BigDecimal employeeTaxTotal,
        BigDecimal employerSocialSecurityTax,
        BigDecimal employerMedicareTax,
        BigDecimal employerFutaTax,
        BigDecimal netPay
    ) {
    }
}
