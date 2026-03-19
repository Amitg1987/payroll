package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.model.Organization;
import com.embeddedpayroll.backend.model.OrganizationJurisdiction;
import com.embeddedpayroll.backend.repository.OrganizationJurisdictionRepository;
import com.embeddedpayroll.backend.repository.OrganizationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationJurisdictionRepository organizationJurisdictionRepository;

    @Transactional(readOnly = true)
    public Organization getOrganization(Long organizationId) {
        return organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));
    }

    @Transactional(readOnly = true)
    public List<OrganizationJurisdiction> listJurisdictions(Long organizationId) {
        return organizationJurisdictionRepository.findByOrganizationIdOrderByPrimaryJurisdictionDescTaxJurisdiction_NameAsc(
            organizationId
        );
    }
}
