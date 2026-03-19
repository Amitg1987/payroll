package com.embeddedpayroll.backend.repository;

import com.embeddedpayroll.backend.model.WebhookDelivery;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    List<WebhookDelivery> findByWebhookEndpointOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    List<WebhookDelivery> findTop20ByDeliveryStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
        List<WebhookDelivery.DeliveryStatus> deliveryStatuses,
        OffsetDateTime nextAttemptAt
    );

    long countByWebhookEndpointOrganizationIdAndDeliveryStatus(
        Long organizationId,
        WebhookDelivery.DeliveryStatus deliveryStatus
    );
}
