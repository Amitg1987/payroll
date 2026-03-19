package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.PayrollRunItemAllocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRunItemAllocationRepository extends JpaRepository<PayrollRunItemAllocation, Long> {

    List<PayrollRunItemAllocation> findByPayrollRunItemIdOrderByStateJurisdictionCodeAscLocalJurisdictionCodeAsc(Long payrollRunItemId);

    List<PayrollRunItemAllocation> findByPayrollRunItemPayrollRunPayrollScheduleOrganizationIdAndPayrollRunItemTaxYear(
        Long organizationId,
        Integer taxYear
    );
}
