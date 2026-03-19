package com.embeddedpayroll.backend.controller;

import com.embeddedpayroll.backend.config.ApiRoutes;
import com.embeddedpayroll.backend.dto.EmployeeDtos;
import com.embeddedpayroll.backend.security.ActorContext;
import com.embeddedpayroll.backend.security.TenantAccessService;
import com.embeddedpayroll.backend.service.IdempotencyService;
import com.embeddedpayroll.backend.service.PayrollService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
    ApiRoutes.LEGACY_API_PREFIX + "/employees",
    ApiRoutes.PARTNER_API_PREFIX + "/employees"
})
@RequiredArgsConstructor
public class EmployeeController {

    private final PayrollService payrollService;
    private final TenantAccessService tenantAccessService;
    private final IdempotencyService idempotencyService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER','DEVELOPER_PLATFORM_INTEGRATOR')")
    public List<EmployeeDtos.EmployeeResponse> employees(@RequestParam Long organizationId, Authentication authentication) {
        tenantAccessService.assertOrganizationAccess(authentication, organizationId);
        int currentTaxYear = LocalDate.now().getYear();
        return payrollService.listEmployees(organizationId).stream()
            .map(employee -> EmployeeDtos.fromEntity(
                employee,
                payrollService.findCurrentW4(employee.getId(), currentTaxYear)
            ))
            .toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','DEVELOPER_PLATFORM_INTEGRATOR')")
    public EmployeeDtos.EmployeeResponse createEmployee(
        @Valid @RequestBody EmployeeDtos.CreateEmployeeRequest request,
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
            ApiRoutes.PARTNER_API_PREFIX + "/employees",
            request,
            EmployeeDtos.EmployeeResponse.class,
            () -> {
                var employee = payrollService.createEmployee(request, actorContext.organizationId());
                return EmployeeDtos.fromEntity(
                    employee,
                    payrollService.findCurrentW4(employee.getId(), request.w4Profile().taxYear())
                );
            }
        );
    }

    @PutMapping("/{employeeId}/w4")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','DEVELOPER_PLATFORM_INTEGRATOR')")
    public EmployeeDtos.W4Response upsertW4(
        @PathVariable Long employeeId,
        @Valid @RequestBody EmployeeDtos.W4Request request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        Authentication authentication
    ) {
        ActorContext actorContext = tenantAccessService.resolveActor(authentication);
        return idempotencyService.execute(
            actorContext.organizationId(),
            actorContext.actorKey(),
            idempotencyKey,
            "PUT",
            ApiRoutes.PARTNER_API_PREFIX + "/employees/" + employeeId + "/w4",
            request,
            EmployeeDtos.W4Response.class,
            () -> EmployeeDtos.fromEntity(payrollService.upsertW4(employeeId, request, actorContext.organizationId()))
        );
    }
}
