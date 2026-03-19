package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.OrganizationJurisdiction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationJurisdictionRepository extends JpaRepository<OrganizationJurisdiction, Long> {

    List<OrganizationJurisdiction> findByOrganizationIdOrderByPrimaryJurisdictionDescTaxJurisdiction_NameAsc(Long organizationId);
}
