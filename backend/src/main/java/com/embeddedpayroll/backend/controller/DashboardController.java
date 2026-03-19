package com.embeddedpayroll.backend.controller;

import com.embeddedpayroll.backend.config.ApiRoutes;
import com.embeddedpayroll.backend.dto.DashboardDtos;
import com.embeddedpayroll.backend.model.PayrollRun;
import com.embeddedpayroll.backend.model.TaxFilingRecord;
import com.embeddedpayroll.backend.repository.EmployeeRepository;
import com.embeddedpayroll.backend.repository.OrganizationRepository;
import com.embeddedpayroll.backend.repository.OrganizationJurisdictionRepository;
import com.embeddedpayroll.backend.repository.PayrollRunRepository;
import com.embeddedpayroll.backend.repository.PayrollScheduleRepository;
import com.embeddedpayroll.backend.repository.TaxFilingRecordRepository;
import com.embeddedpayroll.backend.security.TenantAccessService;
import com.embeddedpayroll.backend.service.PartnerIntegrationService;
import com.embeddedpayroll.backend.service.TaxService;
import com.embeddedpayroll.backend.service.WebhookService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
    ApiRoutes.LEGACY_API_PREFIX + "/dashboard",
    ApiRoutes.PARTNER_API_PREFIX + "/dashboard"
})
@RequiredArgsConstructor
public class DashboardController {

    private final OrganizationRepository organizationRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollScheduleRepository payrollScheduleRepository;
    private final TaxFilingRecordRepository taxFilingRecordRepository;
    private final OrganizationJurisdictionRepository organizationJurisdictionRepository;
    private final TaxService taxService;
    private final PartnerIntegrationService partnerIntegrationService;
    private final WebhookService webhookService;
    private final TenantAccessService tenantAccessService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER','DEVELOPER_PLATFORM_INTEGRATOR')")
    public DashboardDtos.DashboardSummaryResponse summary(@RequestParam Long organizationId, Authentication authentication) {
        tenantAccessService.assertOrganizationAccess(authentication, organizationId);
        var organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));
        var schedules = payrollScheduleRepository.findByOrganizationIdAndActiveTrueOrderByNameAsc(organizationId);
        List<Integer> supportedTaxYears = taxService.listTaxYears().stream()
            .map(profile -> profile.getTaxYear())
            .toList();

        return new DashboardDtos.DashboardSummaryResponse(
            organization.getId(),
            organization.getLegalName(),
            organization.getEin(),
            organization.getPrimaryJurisdictionCode(),
            employeeRepository.countByOrganizationId(organizationId),
            payrollRunRepository.countByPayrollScheduleOrganizationIdAndStatus(
                organizationId,
                PayrollRun.RunStatus.PENDING_APPROVAL
            ),
            taxFilingRecordRepository.countByOrganizationIdAndStatus(
                organizationId,
                TaxFilingRecord.RecordStatus.GENERATED
            ),
            organizationJurisdictionRepository.findByOrganizationIdOrderByPrimaryJurisdictionDescTaxJurisdiction_NameAsc(
                organizationId
            ).size(),
            partnerIntegrationService.countApiClients(organizationId),
            webhookService.countPendingDeliveries(organizationId),
            schedules.isEmpty() ? null : schedules.getFirst().getNextPayDate(),
            supportedTaxYears
        );
    }
}
