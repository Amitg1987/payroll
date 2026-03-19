package com.embeddedpayroll.backend;

import com.embeddedpayroll.backend.model.Employee;
import com.embeddedpayroll.backend.model.EmployeeTaxAccumulator;
import com.embeddedpayroll.backend.model.PayrollSchedule;
import com.embeddedpayroll.backend.repository.EmployeeTaxAccumulatorRepository;
import com.embeddedpayroll.backend.repository.EmployeeRepository;
import com.embeddedpayroll.backend.repository.UserAccountRepository;
import com.embeddedpayroll.backend.dto.PayrollDtos;
import com.embeddedpayroll.backend.service.DemoDataInitializer;
import com.embeddedpayroll.backend.service.PayrollService;
import com.embeddedpayroll.backend.service.TaxEngineService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
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

	@Autowired
	private EmployeeTaxAccumulatorRepository employeeTaxAccumulatorRepository;

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
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			List.of()
		);

		assertThat(calculation.employeeId()).isEqualTo(employee.getId());
		assertThat(calculation.grossPay()).isPositive();
		assertThat(calculation.stateIncomeTax()).isPositive();
		assertThat(calculation.localIncomeTax()).isPositive();
		assertThat(calculation.netPay()).isPositive();
		assertThat(calculation.netPay()).isLessThan(calculation.grossPay());
	}

	@Test
	void reciprocityAgreementWithholdsOnlyResidentStateTax() {
		Employee employee = employeeRepository.findByEmployeeNumber("EMP-1004").orElseThrow();
		var w4Profile = payrollService.findCurrentW4(employee.getId(), 2026);
		var calculation = taxEngineService.calculate(
			employee,
			w4Profile,
			2026,
			PayrollSchedule.PayrollFrequency.BIWEEKLY,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			List.of()
		);

		assertThat(calculation.workStateIncomeTax()).isEqualByComparingTo("0.00");
		assertThat(calculation.residentStateIncomeTax()).isPositive();
		assertThat(calculation.residentStateCreditOffset()).isEqualByComparingTo("0.00");
		assertThat(calculation.residentStateJurisdictionCode()).isEqualTo("NJ");
	}

	@Test
	void nonReciprocalStatesApplyResidentCreditOffset() {
		Employee employee = employeeRepository.findByEmployeeNumber("EMP-1005").orElseThrow();
		var w4Profile = payrollService.findCurrentW4(employee.getId(), 2026);
		var calculation = taxEngineService.calculate(
			employee,
			w4Profile,
			2026,
			PayrollSchedule.PayrollFrequency.BIWEEKLY,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			List.of()
		);

		assertThat(calculation.workStateIncomeTax()).isPositive();
		assertThat(calculation.residentStateIncomeTax()).isPositive();
		assertThat(calculation.residentStateCreditOffset()).isPositive();
		assertThat(calculation.stateIncomeTax())
			.isEqualByComparingTo(calculation.workStateIncomeTax().add(calculation.residentStateIncomeTax()));
	}

	@Test
	void multiStateAllocationsAreTrackedPerWorkLocation() {
		Employee employee = employeeRepository.findByEmployeeNumber("EMP-1005").orElseThrow();
		var w4Profile = payrollService.findCurrentW4(employee.getId(), 2026);
		var calculation = taxEngineService.calculate(
			employee,
			w4Profile,
			2026,
			PayrollSchedule.PayrollFrequency.BIWEEKLY,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			List.of(
				new PayrollDtos.WorkLocationAllocationRequest("NJ", null, new BigDecimal("0.6000")),
				new PayrollDtos.WorkLocationAllocationRequest("NY", "NYC_NY", new BigDecimal("0.4000"))
			)
		);

		assertThat(calculation.workLocationAccumulators()).hasSize(2);
		assertThat(calculation.workLocationAccumulators().stream()
			.map(TaxEngineService.WorkLocationAccumulator::allocatedTaxableWages)
			.reduce(BigDecimal.ZERO, BigDecimal::add))
			.isEqualByComparingTo(calculation.taxableWages());
		assertThat(calculation.workLocationAccumulators().stream()
			.anyMatch(allocation -> allocation.stateJurisdictionCode().equals("NJ") && allocation.workStateIncomeTax().signum() > 0))
			.isTrue();
	}

	@Test
	void ytdAccumulatorsCapWageBaseTaxes() {
		Employee employee = employeeRepository.findByEmployeeNumber("EMP-1003").orElseThrow();
		var w4Profile = payrollService.findCurrentW4(employee.getId(), 2026);

		saveAccumulator(employee, 2026, EmployeeTaxAccumulator.TaxCode.SOCIAL_SECURITY, "US", "181500.00");
		saveAccumulator(employee, 2026, EmployeeTaxAccumulator.TaxCode.FUTA, "US", "6900.00");
		saveAccumulator(employee, 2026, EmployeeTaxAccumulator.TaxCode.STATE_UNEMPLOYMENT, "TX", "11800.00");
		saveAccumulator(employee, 2026, EmployeeTaxAccumulator.TaxCode.MEDICARE, "US", "150000.00");

		var calculation = taxEngineService.calculate(
			employee,
			w4Profile,
			2026,
			PayrollSchedule.PayrollFrequency.BIWEEKLY,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			List.of()
		);

		assertThat(calculation.socialSecurityTaxableWages()).isEqualByComparingTo("500.00");
		assertThat(calculation.federalUnemploymentTaxableWages()).isEqualByComparingTo("100.00");
		assertThat(calculation.stateUnemploymentTaxableWages()).isEqualByComparingTo("200.00");
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

	private void saveAccumulator(
		Employee employee,
		int taxYear,
		EmployeeTaxAccumulator.TaxCode taxCode,
		String jurisdictionCode,
		String taxableWages
	) {
		EmployeeTaxAccumulator accumulator = employeeTaxAccumulatorRepository
			.findByEmployeeIdAndTaxYearAndTaxCodeAndJurisdictionCode(
				employee.getId(),
				taxYear,
				taxCode,
				jurisdictionCode
			)
			.orElseGet(EmployeeTaxAccumulator::new);
		accumulator.setEmployee(employee);
		accumulator.setTaxYear(taxYear);
		accumulator.setTaxCode(taxCode);
		accumulator.setJurisdictionCode(jurisdictionCode);
		accumulator.setYtdTaxableWages(new BigDecimal(taxableWages));
		accumulator.setYtdEmployeeTaxAmount(BigDecimal.ZERO);
		accumulator.setYtdEmployerTaxAmount(BigDecimal.ZERO);
		employeeTaxAccumulatorRepository.save(accumulator);
	}
}
