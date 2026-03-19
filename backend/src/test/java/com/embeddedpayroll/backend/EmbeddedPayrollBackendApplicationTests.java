package com.embeddedpayroll.backend;

import com.embeddedpayroll.backend.model.Employee;
import com.embeddedpayroll.backend.model.PayrollSchedule;
import com.embeddedpayroll.backend.repository.EmployeeRepository;
import com.embeddedpayroll.backend.repository.UserAccountRepository;
import com.embeddedpayroll.backend.service.DemoDataInitializer;
import com.embeddedpayroll.backend.service.PayrollService;
import com.embeddedpayroll.backend.service.TaxEngineService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmbeddedPayrollBackendApplicationTests {

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

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
	void demoUsersAreSeeded() {
		assertThat(userAccountRepository.findByUsername("admin")).isPresent();
		assertThat(userAccountRepository.findByUsername("accountant")).isPresent();
		assertThat(userAccountRepository.findByUsername("approver")).isPresent();
		assertThat(userAccountRepository.findByUsername("integrator")).isPresent();
	}

	@Test
	void payrollCalculationProducesNetPayIncludingStateAndLocalTaxes() {
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
		assertThat(calculation.stateIncomeTax()).isPositive();
		assertThat(calculation.localIncomeTax()).isPositive();
		assertThat(calculation.netPay()).isPositive();
		assertThat(calculation.netPay()).isLessThan(calculation.grossPay());
	}

	@Test
	void partnerVersionedEmployeeEndpointIsAvailable() throws Exception {
		Employee employee = employeeRepository.findByEmployeeNumber("EMP-1001").orElseThrow();
		String response = request(
			"/api/v1/employees?organizationId=" + employee.getOrganization().getId(),
			"GET",
			null,
			Map.of("Authorization", basicAuth("admin", "Admin@123"))
		);

		assertThat(response).contains("EMP-1001");
	}

	@Test
	void partnerOpenApiGroupIsPublished() throws Exception {
		String response = request("/v3/api-docs/partner-v1", "GET", null, Map.of());

		assertThat(response).contains("/api/v1/employees");
		assertThat(response).contains("/api/v1/integration/api-clients");
	}

	@Test
	void apiKeyPartnerAuthCanAccessCurrentActor() throws Exception {
		String response = request(
			"/api/v1/security/me",
			"GET",
			null,
			Map.of("X-API-Key", DemoDataInitializer.DEMO_PARTNER_API_KEY)
		);

		assertThat(response).contains("\"authenticationMode\":\"API_KEY\"");
		assertThat(response).contains("\"partnerClientCode\":\"acme-platform-sdk\"");
	}

	@Test
	void idempotencyKeyPreventsDuplicateEmployeeCreation() throws Exception {
		Employee seededEmployee = employeeRepository.findByEmployeeNumber("EMP-1001").orElseThrow();
		long beforeCount = employeeRepository.countByOrganizationId(seededEmployee.getOrganization().getId());

		String payload = """
			{
			  "organizationId": %d,
			  "employeeNumber": "EMP-2200",
			  "firstName": "Taylor",
			  "lastName": "Brooks",
			  "email": "taylor.brooks@example.com",
			  "ssnLastFour": "9911",
			  "hireDate": "2026-05-02",
			  "employmentStatus": "ACTIVE",
			  "compensationType": "SALARIED",
			  "department": "Product",
			  "workState": "CA",
			  "residenceState": "CA",
			  "workLocalJurisdictionCode": "SF_CA",
			  "residenceLocalJurisdictionCode": "SF_CA",
			  "annualSalary": 93000,
			  "hourlyRate": null,
			  "standardHoursPerPeriod": 80,
			  "stateWithholdingRate": 0.06,
			  "w4Profile": {
			    "taxYear": 2026,
			    "filingStatus": "SINGLE",
			    "multipleJobs": false,
			    "dependentsCredit": 0,
			    "otherIncome": 0,
			    "deductions": 0,
			    "extraWithholding": 0,
			    "exemptFromWithholding": false
			  }
			}
			""".formatted(seededEmployee.getOrganization().getId());

		String headersKey = "idem-create-employee-2200";
		String firstResponse = request(
			"/api/v1/employees",
			"POST",
			payload,
			Map.of(
				"Content-Type", "application/json",
				"Idempotency-Key", headersKey,
				"X-API-Key", DemoDataInitializer.DEMO_PARTNER_API_KEY
			)
		);
		String secondResponse = request(
			"/api/v1/employees",
			"POST",
			payload,
			Map.of(
				"Content-Type", "application/json",
				"Idempotency-Key", headersKey,
				"X-API-Key", DemoDataInitializer.DEMO_PARTNER_API_KEY
			)
		);

		assertThat(firstResponse).contains("\"employeeNumber\":\"EMP-2200\"");
		assertThat(secondResponse).contains("\"employeeNumber\":\"EMP-2200\"");
		assertThat(employeeRepository.countByOrganizationId(seededEmployee.getOrganization().getId()))
			.isEqualTo(beforeCount + 1);
	}

	private String request(
		String path,
		String method,
		String body,
		Map<String, String> headers
	) throws Exception {
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + path));

		headers.forEach(requestBuilder::header);

		if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
			requestBuilder.method(
				method,
				HttpRequest.BodyPublishers.ofString(body == null ? "" : body)
			);
		} else {
			requestBuilder.GET();
		}

		HttpResponse<String> response = HTTP_CLIENT.send(
			requestBuilder.build(),
			HttpResponse.BodyHandlers.ofString()
		);
		assertThat(response.statusCode()).isEqualTo(200);
		return response.body();
	}

	private String basicAuth(String username, String password) {
		return "Basic " + Base64.getEncoder().encodeToString(
			(username + ":" + password).getBytes(StandardCharsets.UTF_8)
		);
	}
}
