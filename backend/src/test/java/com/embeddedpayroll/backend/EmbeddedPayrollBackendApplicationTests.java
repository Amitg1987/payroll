package com.embeddedpayroll.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmbeddedPayrollBackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void currentUserEndpointReturnsSeededAdmin() throws Exception {
		mockMvc.perform(get("/api/security/me")
				.with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "Admin@123")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("admin"))
			.andExpect(jsonPath("$.roles[0]").value("ADMIN"));
	}

	@Test
	void payrollCalculationEndpointProducesNetPay() throws Exception {
		String request = """
			{
			  "employeeId": 1,
			  "taxYear": 2026,
			  "frequency": "BIWEEKLY",
			  "bonusPay": 0,
			  "overtimeHours": 0,
			  "preTaxDeductions": 0
			}
			""";

		mockMvc.perform(post("/api/payroll/calculate")
				.with(SecurityMockMvcRequestPostProcessors.httpBasic("accountant", "Accountant@123"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(request))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.employeeId").value(1))
			.andExpect(jsonPath("$.grossPay").isNumber())
			.andExpect(jsonPath("$.netPay").isNumber());
	}

}
