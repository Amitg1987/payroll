package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.TaxFilingWorkflowRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxFilingWorkflowRequestRepository extends JpaRepository<TaxFilingWorkflowRequest, Long> {

    List<TaxFilingWorkflowRequest> findByOrganizationIdOrderByRequestedAtDesc(Long organizationId);
}
