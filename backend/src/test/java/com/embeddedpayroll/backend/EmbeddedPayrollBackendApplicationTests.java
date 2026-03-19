package com.embeddedpayroll.backend;

import com.embeddedpayroll.backend.model.Employee;
import com.embeddedpayroll.backend.model.PayrollSchedule;
import com.embeddedpayroll.backend.repository.EmployeeRepository;
import com.embeddedpayroll.backend.repository.UserAccountRepository;
import com.embeddedpayroll.backend.service.PayrollService;
import com.embeddedpayroll.backend.service.TaxEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmbeddedPayrollBackendApplicationTests {

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private PayrollService payrollService;

	@Autowired
	private TaxEngineService taxEngineService;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void demoUsersAreSeeded() {
		assertThat(userAccountRepository.findByUsername("admin")).isPresent();
		assertThat(userAccountRepository.findByUsername("accountant")).isPresent();
		assertThat(userAccountRepository.findByUsername("approver")).isPresent();
	}

	@Test
	void payrollCalculationProducesNetPay() {
		Employee employee = employeeRepository.findByEmployeeNumber("EMP-1001").orElseThrow();
		var w4Profile = payrollService.findCurrentW4(employee.getId(), 2026);
		var calculation = taxEngineService.calculate(
			employee,
			w4Profile,
			2026,
			PayrollSchedule.PayrollFrequency.BIWEEKLY,
			java.math.BigDecimal.ZERO,
			java.math.BigDecimal.ZERO,
			java.math.BigDecimal.ZERO
		);

		assertThat(calculation.employeeId()).isEqualTo(employee.getId());
		assertThat(calculation.grossPay()).isPositive();
		assertThat(calculation.netPay()).isPositive();
		assertThat(calculation.netPay()).isLessThan(calculation.grossPay());
	}

	@Test
	void partnerVersionedEmployeeEndpointIsAvailable() {
		Employee employee = employeeRepository.findByEmployeeNumber("EMP-1001").orElseThrow();
		var response = restTemplate.withBasicAuth("admin", "Admin@123")
			.getForEntity("/api/v1/employees?organizationId=" + employee.getOrganization().getId(), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("EMP-1001");
	}

	@Test
	void partnerOpenApiGroupIsPublished() {
		var response = restTemplate.getForEntity("/v3/api-docs/partner-v1", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("/api/v1/employees");
	}

}
