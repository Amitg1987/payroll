package com.embeddedpayroll.backend.dto;

import com.embeddedpayroll.backend.model.FederalTaxBracket;
import com.embeddedpayroll.backend.model.TaxFilingRecord;
import com.embeddedpayroll.backend.model.TaxYearProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

public final class TaxDtos {

    private TaxDtos() {
    }

    public static TaxYearResponse fromEntity(TaxYearProfile profile) {
        List<BracketResponse> brackets = profile.getFederalTaxBrackets().stream()
            .sorted(Comparator.comparing(FederalTaxBracket::getFilingStatus).thenComparing(FederalTaxBracket::getBracketOrder))
            .map(TaxDtos::fromEntity)
            .toList();
        return new TaxYearResponse(
            profile.getTaxYear(),
            profile.getSocialSecurityEmployeeRate(),
            profile.getSocialSecurityEmployerRate(),
            profile.getSocialSecurityWageBase(),
            profile.getMedicareEmployeeRate(),
            profile.getMedicareEmployerRate(),
            profile.getAdditionalMedicareRate(),
            profile.getAdditionalMedicareThreshold(),
            profile.getFederalUnemploymentRate(),
            profile.getFederalUnemploymentWageBase(),
            profile.getDefaultStateUnemploymentRate(),
            profile.getStandardDeductionSingle(),
            profile.getStandardDeductionMarriedJointly(),
            profile.getStandardDeductionHeadOfHousehold(),
            profile.getNotes(),
            brackets
        );
    }

    public static BracketResponse fromEntity(FederalTaxBracket bracket) {
        return new BracketResponse(
            bracket.getFilingStatus(),
            bracket.getBracketOrder(),
            bracket.getLowerBound(),
            bracket.getUpperBound(),
            bracket.getRate()
        );
    }

    public static TaxFilingRecordResponse fromEntity(TaxFilingRecord record) {
        return new TaxFilingRecordResponse(
            record.getId(),
            record.getOrganization().getId(),
            record.getFilingType(),
            record.getTaxYear(),
            record.getFilingPeriod(),
            record.getDueDate(),
            record.getStatus(),
            record.getTotalWages(),
            record.getTotalTax(),
            record.getGeneratedAt(),
            record.getSubmittedAt(),
            record.getReferenceNumber()
        );
    }

    public record GenerateFilingRequest(
        @NotNull Long organizationId,
        @NotNull Integer taxYear,
        @NotNull TaxFilingRecord.FilingType filingType,
        @NotBlank String filingPeriod,
        LocalDate dueDate
    ) {
    }

    public record TaxYearResponse(
        Integer taxYear,
        BigDecimal socialSecurityEmployeeRate,
        BigDecimal socialSecurityEmployerRate,
        BigDecimal socialSecurityWageBase,
        BigDecimal medicareEmployeeRate,
        BigDecimal medicareEmployerRate,
        BigDecimal additionalMedicareRate,
        BigDecimal additionalMedicareThreshold,
        BigDecimal federalUnemploymentRate,
        BigDecimal federalUnemploymentWageBase,
        BigDecimal defaultStateUnemploymentRate,
        BigDecimal standardDeductionSingle,
        BigDecimal standardDeductionMarriedJointly,
        BigDecimal standardDeductionHeadOfHousehold,
        String notes,
        List<BracketResponse> federalTaxBrackets
    ) {
    }

    public record BracketResponse(
        com.embeddedpayroll.backend.model.EmployeeW4Profile.FilingStatus filingStatus,
        Integer bracketOrder,
        BigDecimal lowerBound,
        BigDecimal upperBound,
        BigDecimal rate
    ) {
    }

    public record TaxFilingRecordResponse(
        Long id,
        Long organizationId,
        TaxFilingRecord.FilingType filingType,
        Integer taxYear,
        String filingPeriod,
        LocalDate dueDate,
        TaxFilingRecord.RecordStatus status,
        BigDecimal totalWages,
        BigDecimal totalTax,
        OffsetDateTime generatedAt,
        OffsetDateTime submittedAt,
        String referenceNumber
    ) {
    }
}
