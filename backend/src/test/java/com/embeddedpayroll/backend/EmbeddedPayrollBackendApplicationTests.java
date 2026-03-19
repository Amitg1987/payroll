package com.embeddedpayroll.backend;

import com.embeddedpayroll.backend.model.Employee;
import com.embeddedpayroll.backend.model.PayrollSchedule;
import com.embeddedpayroll.backend.repository.EmployeeRepository;
import com.embeddedpayroll.backend.repository.UserAccountRepository;
import com.embeddedpayroll.backend.service.PayrollService;
import com.embeddedpayroll.backend.service.TaxEngineService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

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

	@LocalServerPort
	private int port;

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
	void partnerVersionedEmployeeEndpointIsAvailable() throws Exception {
		Employee employee = employeeRepository.findByEmployeeNumber("EMP-1001").orElseThrow();
		String response = get(
			"/api/v1/employees?organizationId=" + employee.getOrganization().getId(),
			"admin",
			"Admin@123"
		);

		assertThat(response).contains("EMP-1001");
	}

	@Test
	void partnerOpenApiGroupIsPublished() throws Exception {
		String response = get("/v3/api-docs/partner-v1", null, null);

		assertThat(response).contains("/api/v1/employees");
	}

	private String get(String path, String username, String password) throws Exception {
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + path))
			.GET();

		if (username != null && password != null) {
			String encodedCredentials = Base64.getEncoder().encodeToString(
				(username + ":" + password).getBytes(StandardCharsets.UTF_8)
			);
			requestBuilder.header("Authorization", "Basic " + encodedCredentials);
		}

		HttpResponse<String> response = HttpClient.newHttpClient().send(
			requestBuilder.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		assertThat(response.statusCode()).isEqualTo(200);
		return response.body();
	}

}
