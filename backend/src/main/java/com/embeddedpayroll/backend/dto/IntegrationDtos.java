package com.embeddedpayroll.backend.dto;

import com.embeddedpayroll.backend.model.WebhookDelivery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public final class IntegrationDtos {

    private IntegrationDtos() {
    }

    public record CreateApiClientRequest(
        @NotNull Long organizationId,
        @NotBlank String clientCode,
        @NotBlank String displayName,
        @NotBlank String scopes,
        String description
    ) {
    }

    public record PartnerApiClientResponse(
        Long id,
        Long organizationId,
        String clientCode,
        String displayName,
        String keyPrefix,
        String scopes,
        String description,
        boolean active,
        String issuedApiKey
    ) {
    }

    public record CreateWebhookRequest(
        @NotNull Long organizationId,
        Long partnerApiClientId,
        @NotBlank String name,
        @NotBlank String targetUrl,
        @NotBlank String subscribedEvent
    ) {
    }

    public record WebhookEndpointResponse(
        Long id,
        Long organizationId,
        Long partnerApiClientId,
        String partnerClientCode,
        String name,
        String targetUrl,
        String subscribedEvent,
        boolean active,
        String issuedSigningSecret
    ) {
    }

    public record WebhookDeliveryResponse(
        Long id,
        Long webhookEndpointId,
        String endpointName,
        String eventType,
        String eventKey,
        WebhookDelivery.DeliveryStatus deliveryStatus,
        Integer attempts,
        OffsetDateTime nextAttemptAt,
        OffsetDateTime deliveredAt,
        Integer responseStatusCode,
        String responseBody
    ) {
    }

    public record DispatchWebhookDeliveriesResponse(int processedCount) {
    }
}
