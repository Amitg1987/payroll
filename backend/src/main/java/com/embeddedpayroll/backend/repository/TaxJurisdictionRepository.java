package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.TaxJurisdiction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxJurisdictionRepository extends JpaRepository<TaxJurisdiction, Long> {

    Optional<TaxJurisdiction> findByCode(String code);

    List<TaxJurisdiction> findByJurisdictionTypeOrderByNameAsc(TaxJurisdiction.JurisdictionType jurisdictionType);
}
