package com.embeddedpayroll.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EmbeddedPayrollBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmbeddedPayrollBackendApplication.class, args);
	}

}
