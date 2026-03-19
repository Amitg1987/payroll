package com.embeddedpayroll.backend.controller;

import com.embeddedpayroll.backend.config.ApiRoutes;
import com.embeddedpayroll.backend.dto.IntegrationDtos;
import com.embeddedpayroll.backend.security.ActorContext;
import com.embeddedpayroll.backend.security.TenantAccessService;
import com.embeddedpayroll.backend.service.IdempotencyService;
import com.embeddedpayroll.backend.service.PartnerIntegrationService;
import com.embeddedpayroll.backend.service.WebhookService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
    ApiRoutes.LEGACY_API_PREFIX + "/integration",
    ApiRoutes.PARTNER_API_PREFIX + "/integration"
})
@RequiredArgsConstructor
public class IntegrationController {

    private final PartnerIntegrationService partnerIntegrationService;
    private final TenantAccessService tenantAccessService;
    private final IdempotencyService idempotencyService;
    private final WebhookService webhookService;

    @GetMapping("/api-clients")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER_PLATFORM_INTEGRATOR')")
    public List<IntegrationDtos.PartnerApiClientResponse> apiClients(
        @RequestParam Long organizationId,
        Authentication authentication
    ) {
        tenantAccessService.assertOrganizationAccess(authentication, organizationId);
        return partnerIntegrationService.listApiClients(organizationId).stream()
            .map(client -> new IntegrationDtos.PartnerApiClientResponse(
                client.getId(),
                client.getOrganization().getId(),
                client.getClientCode(),
                client.getDisplayName(),
                client.getKeyPrefix(),
                client.getScopes(),
                client.getDescription(),
                client.isActive(),
                null
            ))
            .toList();
    }

    @PostMapping("/api-clients")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER_PLATFORM_INTEGRATOR')")
    public IntegrationDtos.PartnerApiClientResponse createApiClient(
        @Valid @RequestBody IntegrationDtos.CreateApiClientRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        Authentication authentication
    ) {
        ActorContext actorContext = tenantAccessService.resolveActor(authentication);
        tenantAccessService.assertOrganizationAccess(authentication, request.organizationId());
        return idempotencyService.execute(
            actorContext.organizationId(),
            actorContext.actorKey(),
            idempotencyKey,
            "POST",
            ApiRoutes.PARTNER_API_PREFIX + "/integration/api-clients",
            request,
            IntegrationDtos.PartnerApiClientResponse.class,
            () -> {
                PartnerIntegrationService.IssuedApiClient issuedApiClient = partnerIntegrationService.createApiClient(request);
                var client = issuedApiClient.partnerApiClient();
                return new IntegrationDtos.PartnerApiClientResponse(
                    client.getId(),
                    client.getOrganization().getId(),
                    client.getClientCode(),
                    client.getDisplayName(),
                    client.getKeyPrefix(),
                    client.getScopes(),
                    client.getDescription(),
                    client.isActive(),
                    issuedApiClient.rawApiKey()
                );
            }
        );
    }

    @GetMapping("/webhooks")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER_PLATFORM_INTEGRATOR')")
    public List<IntegrationDtos.WebhookEndpointResponse> webhookEndpoints(
        @RequestParam Long organizationId,
        Authentication authentication
    ) {
        tenantAccessService.assertOrganizationAccess(authentication, organizationId);
        return partnerIntegrationService.listWebhookEndpoints(organizationId).stream()
            .map(webhookEndpoint -> new IntegrationDtos.WebhookEndpointResponse(
                webhookEndpoint.getId(),
                webhookEndpoint.getOrganization().getId(),
                webhookEndpoint.getPartnerApiClient() == null ? null : webhookEndpoint.getPartnerApiClient().getId(),
                webhookEndpoint.getPartnerApiClient() == null ? null : webhookEndpoint.getPartnerApiClient().getClientCode(),
                webhookEndpoint.getName(),
                webhookEndpoint.getTargetUrl(),
                webhookEndpoint.getSubscribedEvent(),
                webhookEndpoint.isActive(),
                null
            ))
            .toList();
    }

    @PostMapping("/webhooks")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER_PLATFORM_INTEGRATOR')")
    public IntegrationDtos.WebhookEndpointResponse createWebhook(
        @Valid @RequestBody IntegrationDtos.CreateWebhookRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        Authentication authentication
    ) {
        ActorContext actorContext = tenantAccessService.resolveActor(authentication);
        tenantAccessService.assertOrganizationAccess(authentication, request.organizationId());
        return idempotencyService.execute(
            actorContext.organizationId(),
            actorContext.actorKey(),
            idempotencyKey,
            "POST",
            ApiRoutes.PARTNER_API_PREFIX + "/integration/webhooks",
            request,
            IntegrationDtos.WebhookEndpointResponse.class,
            () -> {
                PartnerIntegrationService.IssuedWebhookEndpoint issuedWebhookEndpoint = partnerIntegrationService.createWebhook(request);
                var endpoint = issuedWebhookEndpoint.webhookEndpoint();
                return new IntegrationDtos.WebhookEndpointResponse(
                    endpoint.getId(),
                    endpoint.getOrganization().getId(),
                    endpoint.getPartnerApiClient() == null ? null : endpoint.getPartnerApiClient().getId(),
                    endpoint.getPartnerApiClient() == null ? null : endpoint.getPartnerApiClient().getClientCode(),
                    endpoint.getName(),
                    endpoint.getTargetUrl(),
                    endpoint.getSubscribedEvent(),
                    endpoint.isActive(),
                    issuedWebhookEndpoint.signingSecret()
                );
            }
        );
    }

    @GetMapping("/webhook-deliveries")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER_PLATFORM_INTEGRATOR')")
    public List<IntegrationDtos.WebhookDeliveryResponse> webhookDeliveries(
        @RequestParam Long organizationId,
        Authentication authentication
    ) {
        tenantAccessService.assertOrganizationAccess(authentication, organizationId);
        return partnerIntegrationService.listWebhookDeliveries(organizationId).stream()
            .map(delivery -> new IntegrationDtos.WebhookDeliveryResponse(
                delivery.getId(),
                delivery.getWebhookEndpoint().getId(),
                delivery.getWebhookEndpoint().getName(),
                delivery.getEventType(),
                delivery.getEventKey(),
                delivery.getDeliveryStatus(),
                delivery.getAttempts(),
                delivery.getNextAttemptAt(),
                delivery.getDeliveredAt(),
                delivery.getResponseStatusCode(),
                delivery.getResponseBody()
            ))
            .toList();
    }

    @PostMapping("/webhook-deliveries/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER_PLATFORM_INTEGRATOR')")
    public IntegrationDtos.DispatchWebhookDeliveriesResponse dispatchWebhookDeliveries() {
        return new IntegrationDtos.DispatchWebhookDeliveriesResponse(webhookService.dispatchPendingDeliveries());
    }
}
