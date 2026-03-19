package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByOrganizationIdOrderByLastNameAscFirstNameAsc(Long organizationId);

    Optional<Employee> findByEmployeeNumber(String employeeNumber);

    long countByOrganizationId(Long organizationId);
}
