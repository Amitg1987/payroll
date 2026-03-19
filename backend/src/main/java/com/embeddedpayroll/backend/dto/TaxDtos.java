package com.embeddedpayroll.backend.dto;

import com.embeddedpayroll.backend.model.FederalTaxBracket;
import com.embeddedpayroll.backend.model.JurisdictionTaxProfile;
import com.embeddedpayroll.backend.model.TaxFilingRecord;
import com.embeddedpayroll.backend.model.TaxFilingWorkflowRequest;
import com.embeddedpayroll.backend.model.TaxJurisdiction;
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
            record.getFilingJurisdictionCode(),
            record.getDueDate(),
            record.getStatus(),
            record.getTotalWages(),
            record.getTotalTax(),
            record.getGeneratedAt(),
            record.getSubmittedAt(),
            record.getReferenceNumber(),
            record.getTemporalWorkflowId(),
            record.getTemporalRunId()
        );
    }

    public static TaxJurisdictionResponse fromEntity(TaxJurisdiction jurisdiction) {
        return new TaxJurisdictionResponse(
            jurisdiction.getCode(),
            jurisdiction.getName(),
            jurisdiction.getJurisdictionType(),
            jurisdiction.getCountryCode(),
            jurisdiction.getStateCode(),
            jurisdiction.getParentJurisdictionCode(),
            jurisdiction.isActive()
        );
    }

    public static JurisdictionTaxProfileResponse fromEntity(JurisdictionTaxProfile profile) {
        return new JurisdictionTaxProfileResponse(
            profile.getTaxJurisdiction().getCode(),
            profile.getTaxJurisdiction().getName(),
            profile.getTaxYear(),
            profile.getTaxType(),
            profile.getResidentRate(),
            profile.getNonResidentRate(),
            profile.getEmployerRate(),
            profile.getWageBase(),
            profile.getStandardDeduction(),
            profile.getNotes()
        );
    }

    public static TaxFilingWorkflowResponse fromEntity(TaxFilingWorkflowRequest request) {
        return new TaxFilingWorkflowResponse(
            request.getId(),
            request.getOrganization().getId(),
            request.getTaxYear(),
            request.getFilingPeriod(),
            request.getRequestedFilings(),
            request.getRequestedBy(),
            request.getWorkflowId(),
            request.getWorkflowRunId(),
            request.getWorkflowStatus(),
            request.getRequestedAt(),
            request.getWorkflowMessage()
        );
    }

    public record GenerateFilingRequest(
        @NotNull Long organizationId,
        @NotNull Integer taxYear,
        @NotNull TaxFilingRecord.FilingType filingType,
        @NotBlank String filingPeriod,
        String filingJurisdictionCode,
        LocalDate dueDate
    ) {
    }

    public record StartFilingWorkflowRequest(
        @NotNull Long organizationId,
        @NotNull Integer taxYear,
        @NotBlank String filingPeriod,
        @NotNull List<TaxFilingRecord.FilingType> filingTypes
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
        String filingJurisdictionCode,
        LocalDate dueDate,
        TaxFilingRecord.RecordStatus status,
        BigDecimal totalWages,
        BigDecimal totalTax,
        OffsetDateTime generatedAt,
        OffsetDateTime submittedAt,
        String referenceNumber,
        String temporalWorkflowId,
        String temporalRunId
    ) {
    }

    public record TaxJurisdictionResponse(
        String code,
        String name,
        TaxJurisdiction.JurisdictionType jurisdictionType,
        String countryCode,
        String stateCode,
        String parentJurisdictionCode,
        boolean active
    ) {
    }

    public record JurisdictionTaxProfileResponse(
        String jurisdictionCode,
        String jurisdictionName,
        Integer taxYear,
        JurisdictionTaxProfile.TaxType taxType,
        BigDecimal residentRate,
        BigDecimal nonResidentRate,
        BigDecimal employerRate,
        BigDecimal wageBase,
        BigDecimal standardDeduction,
        String notes
    ) {
    }

    public record TaxFilingWorkflowResponse(
        Long id,
        Long organizationId,
        Integer taxYear,
        String filingPeriod,
        String requestedFilings,
        String requestedBy,
        String workflowId,
        String workflowRunId,
        TaxFilingWorkflowRequest.WorkflowStatus workflowStatus,
        OffsetDateTime requestedAt,
        String workflowMessage
    ) {
    }
}
