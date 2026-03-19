package com.embeddedpayroll.backend.controller;

import com.embeddedpayroll.backend.dto.PayrollDtos;
import com.embeddedpayroll.backend.service.PayrollService;
import com.embeddedpayroll.backend.service.TaxEngineService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping("/schedules")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER')")
    public List<PayrollDtos.ScheduleResponse> schedules(@RequestParam Long organizationId) {
        return payrollService.listSchedules(organizationId).stream()
            .map(PayrollDtos::fromEntity)
            .toList();
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public PayrollDtos.ScheduleResponse createSchedule(@Valid @RequestBody PayrollDtos.CreateScheduleRequest request) {
        return PayrollDtos.fromEntity(payrollService.createSchedule(request));
    }

    @PostMapping("/schedules/{scheduleId}/process")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public PayrollDtos.PayrollRunResponse processSchedule(
        @PathVariable Long scheduleId,
        @RequestBody(required = false) PayrollDtos.ProcessScheduleRequest request,
        Authentication authentication
    ) {
        PayrollDtos.ProcessScheduleRequest effectiveRequest = request == null
            ? new PayrollDtos.ProcessScheduleRequest(null, null, List.of())
            : request;
        var run = payrollService.processSchedule(scheduleId, effectiveRequest, authentication.getName());
        return toRunResponse(run.getId());
    }

    @GetMapping("/payroll-runs")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER')")
    public List<PayrollDtos.PayrollRunResponse> payrollRuns(@RequestParam Long organizationId) {
        return payrollService.listRuns(organizationId).stream()
            .map(run -> toRunResponse(run.getId()))
            .toList();
    }

    @PostMapping("/payroll-runs/{runId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER')")
    public PayrollDtos.PayrollRunResponse approvePayrollRun(@PathVariable Long runId, Authentication authentication) {
        payrollService.approveRun(runId, authentication.getName());
        return toRunResponse(runId);
    }

    @PostMapping("/payroll/calculate")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER')")
    public PayrollDtos.PayrollCalculationResponse calculatePayroll(
        @Valid @RequestBody PayrollDtos.PayrollCalculationRequest request
    ) {
        TaxEngineService.PayrollComputation computation = payrollService.calculatePayroll(request);
        return new PayrollDtos.PayrollCalculationResponse(
            computation.employeeId(),
            computation.employeeName(),
            computation.taxYear(),
            computation.filingStatus(),
            computation.grossPay(),
            computation.taxableWages(),
            computation.federalIncomeTax(),
            computation.socialSecurityEmployeeTax(),
            computation.medicareEmployeeTax(),
            computation.additionalMedicareEmployeeTax(),
            computation.stateIncomeTax(),
            computation.employeeTaxTotal(),
            computation.employerSocialSecurityTax(),
            computation.employerMedicareTax(),
            computation.employerFutaTax(),
            computation.netPay()
        );
    }

    private PayrollDtos.PayrollRunResponse toRunResponse(Long runId) {
        var run = payrollService.getRun(runId);
        return PayrollDtos.fromEntity(
            run,
            payrollService.listRunItems(runId).stream().map(PayrollDtos::fromEntity).toList()
        );
    }
}
