package com.embeddedpayroll.backend.dto;

import com.embeddedpayroll.backend.model.Organization;
import com.embeddedpayroll.backend.model.OrganizationJurisdiction;
import java.util.List;

public final class OrganizationDtos {

    private OrganizationDtos() {
    }

    public static OrganizationResponse fromEntity(
        Organization organization,
        List<OrganizationJurisdictionResponse> jurisdictions
    ) {
        return new OrganizationResponse(
            organization.getId(),
            organization.getTenantKey(),
            organization.getCode(),
            organization.getLegalName(),
            organization.getDbaName(),
            organization.getEin(),
            organization.getCountryCode(),
            organization.getDefaultStateCode(),
            organization.getPrimaryJurisdictionCode(),
            organization.getHeadquartersLocalJurisdictionCode(),
            organization.getContactEmail(),
            jurisdictions
        );
    }

    public static OrganizationJurisdictionResponse fromEntity(OrganizationJurisdiction organizationJurisdiction) {
        return new OrganizationJurisdictionResponse(
            organizationJurisdiction.getId(),
            organizationJurisdiction.getTaxJurisdiction().getCode(),
            organizationJurisdiction.getTaxJurisdiction().getName(),
            organizationJurisdiction.getTaxJurisdiction().getJurisdictionType(),
            organizationJurisdiction.getNexusType(),
            organizationJurisdiction.getRegistrationNumber(),
            organizationJurisdiction.getEmployerAccountNumber(),
            organizationJurisdiction.isPrimaryJurisdiction()
        );
    }

    public record OrganizationResponse(
        Long id,
        String tenantKey,
        String code,
        String legalName,
        String dbaName,
        String ein,
        String countryCode,
        String defaultStateCode,
        String primaryJurisdictionCode,
        String headquartersLocalJurisdictionCode,
        String contactEmail,
        List<OrganizationJurisdictionResponse> jurisdictions
    ) {
    }

    public record OrganizationJurisdictionResponse(
        Long id,
        String jurisdictionCode,
        String jurisdictionName,
        com.embeddedpayroll.backend.model.TaxJurisdiction.JurisdictionType jurisdictionType,
        OrganizationJurisdiction.NexusType nexusType,
        String registrationNumber,
        String employerAccountNumber,
        boolean primaryJurisdiction
    ) {
    }
}
