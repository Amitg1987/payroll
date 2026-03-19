package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.PayrollRun;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {

    List<PayrollRun> findByPayrollScheduleOrganizationIdOrderByPayDateDesc(Long organizationId);

    long countByPayrollScheduleOrganizationIdAndStatus(Long organizationId, PayrollRun.RunStatus status);
}
