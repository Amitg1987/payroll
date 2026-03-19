package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.WebhookEndpoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, Long> {

    List<WebhookEndpoint> findByOrganizationIdOrderByNameAsc(Long organizationId);

    List<WebhookEndpoint> findByOrganizationIdAndActiveTrue(Long organizationId);
}
