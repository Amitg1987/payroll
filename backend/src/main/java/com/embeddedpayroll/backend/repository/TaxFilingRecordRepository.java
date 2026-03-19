package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.TaxFilingRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxFilingRecordRepository extends JpaRepository<TaxFilingRecord, Long> {

    List<TaxFilingRecord> findByOrganizationIdAndTaxYearOrderByDueDateAsc(Long organizationId, Integer taxYear);

    long countByOrganizationIdAndStatus(Long organizationId, TaxFilingRecord.RecordStatus status);
}
