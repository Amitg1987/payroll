package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.TaxYearProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxYearProfileRepository extends JpaRepository<TaxYearProfile, Long> {

    Optional<TaxYearProfile> findByTaxYear(Integer taxYear);

    List<TaxYearProfile> findAllByOrderByTaxYearDesc();
}
