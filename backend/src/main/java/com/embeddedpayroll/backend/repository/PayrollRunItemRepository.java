package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.PayrollRunItem;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollRunItemRepository extends JpaRepository<PayrollRunItem, Long> {

    List<PayrollRunItem> findByPayrollRunIdOrderByEmployeeLastNameAscEmployeeFirstNameAsc(Long payrollRunId);

    List<PayrollRunItem> findByPayrollRunPayrollScheduleOrganizationIdAndTaxYear(Long organizationId, Integer taxYear);

    @Query("""
        select coalesce(sum(item.grossPay), 0)
        from PayrollRunItem item
        where item.employee.id = :employeeId
          and item.taxYear = :taxYear
        """)
    BigDecimal sumGrossPayForEmployeeAndTaxYear(
        @Param("employeeId") Long employeeId,
        @Param("taxYear") Integer taxYear
    );
}
