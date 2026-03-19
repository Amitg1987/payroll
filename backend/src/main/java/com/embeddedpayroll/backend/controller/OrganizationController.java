package com.embeddedpayroll.backend.controller;

import com.embeddedpayroll.backend.config.ApiRoutes;
import com.embeddedpayroll.backend.dto.OrganizationDtos;
import com.embeddedpayroll.backend.security.ActorContext;
import com.embeddedpayroll.backend.security.TenantAccessService;
import com.embeddedpayroll.backend.service.OrganizationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
    ApiRoutes.LEGACY_API_PREFIX + "/organizations",
    ApiRoutes.PARTNER_API_PREFIX + "/organizations"
})
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final TenantAccessService tenantAccessService;

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER','DEVELOPER_PLATFORM_INTEGRATOR')")
    public OrganizationDtos.OrganizationResponse currentOrganization(Authentication authentication) {
        ActorContext actorContext = tenantAccessService.resolveActor(authentication);
        List<OrganizationDtos.OrganizationJurisdictionResponse> jurisdictions = organizationService
            .listJurisdictions(actorContext.organizationId())
            .stream()
            .map(OrganizationDtos::fromEntity)
            .toList();
        return OrganizationDtos.fromEntity(
            organizationService.getOrganization(actorContext.organizationId()),
            jurisdictions
        );
    }
}
