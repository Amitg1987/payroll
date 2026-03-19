package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.JurisdictionTaxProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JurisdictionTaxProfileRepository extends JpaRepository<JurisdictionTaxProfile, Long> {

    Optional<JurisdictionTaxProfile> findByTaxJurisdiction_CodeAndTaxYearAndTaxType(
        String jurisdictionCode,
        Integer taxYear,
        JurisdictionTaxProfile.TaxType taxType
    );

    List<JurisdictionTaxProfile> findByTaxYearAndTaxTypeOrderByTaxJurisdiction_NameAsc(
        Integer taxYear,
        JurisdictionTaxProfile.TaxType taxType
    );
}
