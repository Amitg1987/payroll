package com.embeddedpayroll.backend.controller;

import com.embeddedpayroll.backend.config.ApiRoutes;
import com.embeddedpayroll.backend.dto.TaxDtos;
import com.embeddedpayroll.backend.service.TaxService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
    ApiRoutes.LEGACY_API_PREFIX + "/tax",
    ApiRoutes.PARTNER_API_PREFIX + "/tax"
})
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;

    @GetMapping("/years")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER')")
    public List<TaxDtos.TaxYearResponse> taxYears() {
        return taxService.listTaxYears().stream()
            .map(TaxDtos::fromEntity)
            .toList();
    }

    @GetMapping("/filings")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','APPROVER')")
    public List<TaxDtos.TaxFilingRecordResponse> filings(@RequestParam Long organizationId, @RequestParam Integer taxYear) {
        return taxService.listFilings(organizationId, taxYear).stream()
            .map(TaxDtos::fromEntity)
            .toList();
    }

    @PostMapping("/filings/generate")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public TaxDtos.TaxFilingRecordResponse generateFiling(@Valid @RequestBody TaxDtos.GenerateFilingRequest request) {
        return TaxDtos.fromEntity(taxService.generateFiling(request));
    }
}
