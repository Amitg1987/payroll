package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.PayrollSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollScheduleRepository extends JpaRepository<PayrollSchedule, Long> {

    List<PayrollSchedule> findByOrganizationIdAndActiveTrueOrderByNameAsc(Long organizationId);
}
