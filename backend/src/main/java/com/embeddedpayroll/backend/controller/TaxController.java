package com.embeddedpayroll.backend.controller;

import com.embeddedpayroll.backend.config.ApiRoutes;
import com.embeddedpayroll.backend.dto.TaxDtos;
import com.embeddedpayroll.backend.model.JurisdictionTaxProfile;
import com.embeddedpayroll.backend.security.ActorContext;
import com.embeddedpayroll.backend.security.TenantAccessService;
import com.embeddedpayroll.backend.service.IdempotencyService;
import com.embeddedpayroll.backend.service.TaxFilingWorkflowService;
import com.embeddedpayroll.backend.service.TaxService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
    ApiRoutes.LEGACY_API_PREFIX + "/tax",
    ApiRoutes.PARTNER_API_PREFIX + "/tax"
})
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;
    private final TaxFilingWorkflowService taxFilingWorkflowService;
    private final TenantAccessService tenantAccessService;
    private final IdempotencyService idempotencyService;

    @GetMapping("/years")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER','DEVELOPER_PLATFORM_INTEGRATOR')")
    public List<TaxDtos.TaxYearResponse> taxYears() {
        return taxService.listTaxYears().stream()
            .map(TaxDtos::fromEntity)
            .toList();
    }

    @GetMapping("/jurisdictions")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER','DEVELOPER_PLATFORM_INTEGRATOR')")
    public List<TaxDtos.TaxJurisdictionResponse> jurisdictions() {
        return taxService.listJurisdictions().stream().map(TaxDtos::fromEntity).toList();
    }

    @GetMapping("/jurisdiction-profiles")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER','DEVELOPER_PLATFORM_INTEGRATOR')")
    public List<TaxDtos.JurisdictionTaxProfileResponse> jurisdictionProfiles(
        @RequestParam Integer taxYear,
        @RequestParam JurisdictionTaxProfile.TaxType taxType
    ) {
        return taxService.listJurisdictionProfiles(taxYear, taxType).stream()
            .map(TaxDtos::fromEntity)
            .toList();
    }

    @GetMapping("/filings")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER','DEVELOPER_PLATFORM_INTEGRATOR')")
    public List<TaxDtos.TaxFilingRecordResponse> filings(
        @RequestParam Long organizationId,
        @RequestParam Integer taxYear,
        Authentication authentication
    ) {
        tenantAccessService.assertOrganizationAccess(authentication, organizationId);
        return taxService.listFilings(organizationId, taxYear).stream()
            .map(TaxDtos::fromEntity)
            .toList();
    }

    @PostMapping("/filings/generate")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','DEVELOPER_PLATFORM_INTEGRATOR')")
    public TaxDtos.TaxFilingRecordResponse generateFiling(
        @Valid @RequestBody TaxDtos.GenerateFilingRequest request,
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
            ApiRoutes.PARTNER_API_PREFIX + "/tax/filings/generate",
            request,
            TaxDtos.TaxFilingRecordResponse.class,
            () -> TaxDtos.fromEntity(taxService.generateFiling(request, actorContext.actorKey(), null, null))
        );
    }

    @GetMapping("/workflows/filings")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER','DEVELOPER_PLATFORM_INTEGRATOR')")
    public List<TaxDtos.TaxFilingWorkflowResponse> filingWorkflows(
        @RequestParam Long organizationId,
        Authentication authentication
    ) {
        tenantAccessService.assertOrganizationAccess(authentication, organizationId);
        return taxFilingWorkflowService.listRequests(organizationId).stream()
            .map(TaxDtos::fromEntity)
            .toList();
    }

    @PostMapping("/workflows/filings")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','DEVELOPER_PLATFORM_INTEGRATOR')")
    public TaxDtos.TaxFilingWorkflowResponse startFilingWorkflow(
        @Valid @RequestBody TaxDtos.StartFilingWorkflowRequest request,
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
            ApiRoutes.PARTNER_API_PREFIX + "/tax/workflows/filings",
            request,
            TaxDtos.TaxFilingWorkflowResponse.class,
            () -> TaxDtos.fromEntity(taxFilingWorkflowService.startWorkflow(request, actorContext.actorKey()))
        );
    }
}
