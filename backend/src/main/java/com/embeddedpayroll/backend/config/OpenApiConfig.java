package com.embeddedpayroll.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI payrollOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Embedded Payroll Partner API")
                .version("v1")
                .description("""
                    Versioned partner-facing API for embedded US payroll, tax, W-4,
                    approval, and filing workflows. Consumers should prefer /api/v1/*
                    routes. Legacy /api/* routes remain available for backward compatibility.
                    """)
                .license(new License().name("Internal reference implementation")))
            .components(new Components().addSecuritySchemes(
                "basicAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")
            ))
            .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }

    @Bean
    GroupedOpenApi partnerV1OpenApi() {
        return GroupedOpenApi.builder()
            .group("partner-v1")
            .pathsToMatch(ApiRoutes.PARTNER_API_PREFIX + "/**")
            .build();
    }

    @Bean
    GroupedOpenApi legacyOpenApi() {
        return GroupedOpenApi.builder()
            .group("legacy")
            .pathsToMatch(ApiRoutes.LEGACY_API_PREFIX + "/**")
            .pathsToExclude(ApiRoutes.PARTNER_API_PREFIX + "/**")
            .build();
    }
}
