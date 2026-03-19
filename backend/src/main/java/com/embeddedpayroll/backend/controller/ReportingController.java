package com.embeddedpayroll.backend.controller;

import com.embeddedpayroll.backend.config.ApiRoutes;
import com.embeddedpayroll.backend.dto.ReportDtos;
import com.embeddedpayroll.backend.security.TenantAccessService;
import com.embeddedpayroll.backend.service.PayrollReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
    ApiRoutes.LEGACY_API_PREFIX + "/reports",
    ApiRoutes.PARTNER_API_PREFIX + "/reports"
})
@RequiredArgsConstructor
public class ReportingController {

    private final PayrollReportingService payrollReportingService;
    private final TenantAccessService tenantAccessService;

    @GetMapping("/payroll-summary")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER','DEVELOPER_PLATFORM_INTEGRATOR')")
    public ReportDtos.PayrollSummaryReportResponse payrollSummary(
        @RequestParam Long organizationId,
        @RequestParam Integer taxYear,
        Authentication authentication
    ) {
        tenantAccessService.assertOrganizationAccess(authentication, organizationId);
        return payrollReportingService.payrollSummary(organizationId, taxYear);
    }
}
