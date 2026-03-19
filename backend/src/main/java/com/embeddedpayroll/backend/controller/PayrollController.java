package com.embeddedpayroll.backend.controller;

import com.embeddedpayroll.backend.config.ApiRoutes;
import com.embeddedpayroll.backend.dto.PayrollDtos;
import com.embeddedpayroll.backend.security.ActorContext;
import com.embeddedpayroll.backend.security.TenantAccessService;
import com.embeddedpayroll.backend.service.IdempotencyService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
    ApiRoutes.LEGACY_API_PREFIX,
    ApiRoutes.PARTNER_API_PREFIX
})
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final TenantAccessService tenantAccessService;
    private final IdempotencyService idempotencyService;

    @GetMapping("/schedules")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER','DEVELOPER_PLATFORM_INTEGRATOR')")
    public List<PayrollDtos.ScheduleResponse> schedules(@RequestParam Long organizationId, Authentication authentication) {
        tenantAccessService.assertOrganizationAccess(authentication, organizationId);
        return payrollService.listSchedules(organizationId).stream()
            .map(PayrollDtos::fromEntity)
            .toList();
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','DEVELOPER_PLATFORM_INTEGRATOR')")
    public PayrollDtos.ScheduleResponse createSchedule(
        @Valid @RequestBody PayrollDtos.CreateScheduleRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        Authentication authentication
    ) {
        ActorContext actorContext = tenantAccessService.resolveActor(authentication);
        tenantAccessService.assertOrganizationAccess(authentication, request.organizationId());
        return idempotencyService.execute(
            actorContext.organizationId(),
            actorContext.actorKey(),
            idempotencyKey,
            "POST",
            ApiRoutes.PARTNER_API_PREFIX + "/schedules",
            request,
            PayrollDtos.ScheduleResponse.class,
            () -> PayrollDtos.fromEntity(payrollService.createSchedule(request, actorContext.organizationId()))
        );
    }

    @PostMapping("/schedules/{scheduleId}/process")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','DEVELOPER_PLATFORM_INTEGRATOR')")
    public PayrollDtos.PayrollRunResponse processSchedule(
        @PathVariable Long scheduleId,
        @RequestBody(required = false) PayrollDtos.ProcessScheduleRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        Authentication authentication
    ) {
        ActorContext actorContext = tenantAccessService.resolveActor(authentication);
        PayrollDtos.ProcessScheduleRequest effectiveRequest = request == null
            ? new PayrollDtos.ProcessScheduleRequest(null, null, List.of())
            : request;
        return idempotencyService.execute(
            actorContext.organizationId(),
            actorContext.actorKey(),
            idempotencyKey,
            "POST",
            ApiRoutes.PARTNER_API_PREFIX + "/schedules/" + scheduleId + "/process",
            effectiveRequest,
            PayrollDtos.PayrollRunResponse.class,
            () -> {
                var run = payrollService.processSchedule(
                    scheduleId,
                    effectiveRequest,
                    actorContext.actorKey(),
                    actorContext.organizationId()
                );
                return toRunResponse(run.getId());
            }
        );
    }

    @GetMapping("/payroll-runs")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER','DEVELOPER_PLATFORM_INTEGRATOR')")
    public List<PayrollDtos.PayrollRunResponse> payrollRuns(@RequestParam Long organizationId, Authentication authentication) {
        tenantAccessService.assertOrganizationAccess(authentication, organizationId);
        return payrollService.listRuns(organizationId).stream()
            .map(run -> toRunResponse(run.getId()))
            .toList();
    }

    @PostMapping("/payroll-runs/{runId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER')")
    public PayrollDtos.PayrollRunResponse approvePayrollRun(@PathVariable Long runId, Authentication authentication) {
        ActorContext actorContext = tenantAccessService.resolveActor(authentication);
        payrollService.approveRun(runId, actorContext.actorKey(), actorContext.organizationId());
        return toRunResponse(runId);
    }

    @PostMapping("/payroll/calculate")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER','DEVELOPER_PLATFORM_INTEGRATOR')")
    public PayrollDtos.PayrollCalculationResponse calculatePayroll(
        @Valid @RequestBody PayrollDtos.PayrollCalculationRequest request,
        Authentication authentication
    ) {
        ActorContext actorContext = tenantAccessService.resolveActor(authentication);
        TaxEngineService.PayrollComputation computation = payrollService.calculatePayroll(
            request,
            actorContext.organizationId()
        );
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
            computation.localIncomeTax(),
            computation.employeeTaxTotal(),
            computation.employerSocialSecurityTax(),
            computation.employerMedicareTax(),
            computation.employerFutaTax(),
            computation.employerStateUnemploymentTax(),
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
