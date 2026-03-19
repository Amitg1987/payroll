package com.embeddedpayroll.backend.temporal;

import com.embeddedpayroll.backend.dto.TaxDtos;
import com.embeddedpayroll.backend.model.TaxFilingRecord;
import com.embeddedpayroll.backend.service.TaxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TaxFilingActivitiesImpl implements TaxFilingActivities {

    private final TaxService taxService;

    @Override
    @Transactional
    public void generateFiling(
        Long organizationId,
        Integer taxYear,
        String filingPeriod,
        String filingType,
        String requestedBy,
        String workflowId
    ) {
        taxService.generateFiling(
            new TaxDtos.GenerateFilingRequest(
                organizationId,
                taxYear,
                TaxFilingRecord.FilingType.valueOf(filingType),
                filingPeriod,
                null,
                null
            ),
            requestedBy,
            workflowId,
            null
        );
    }
}
