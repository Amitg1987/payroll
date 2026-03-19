package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.EmployeeTaxAccumulator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeTaxAccumulatorRepository extends JpaRepository<EmployeeTaxAccumulator, Long> {

    Optional<EmployeeTaxAccumulator> findByEmployeeIdAndTaxYearAndTaxCodeAndJurisdictionCode(
        Long employeeId,
        Integer taxYear,
        EmployeeTaxAccumulator.TaxCode taxCode,
        String jurisdictionCode
    );

    List<EmployeeTaxAccumulator> findByEmployeeIdAndTaxYearOrderByTaxCodeAscJurisdictionCodeAsc(
        Long employeeId,
        Integer taxYear
    );
}
