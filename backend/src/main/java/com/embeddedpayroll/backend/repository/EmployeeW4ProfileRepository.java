package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.EmployeeW4Profile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeW4ProfileRepository extends JpaRepository<EmployeeW4Profile, Long> {

    Optional<EmployeeW4Profile> findByEmployeeIdAndTaxYear(Long employeeId, Integer taxYear);

    Optional<EmployeeW4Profile> findTopByEmployeeIdAndTaxYearLessThanEqualOrderByTaxYearDesc(Long employeeId, Integer taxYear);

    List<EmployeeW4Profile> findByEmployeeOrganizationIdOrderByEmployeeLastNameAscEmployeeFirstNameAsc(Long organizationId);
}
