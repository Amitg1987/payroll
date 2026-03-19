package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.dto.IntegrationDtos;
import com.embeddedpayroll.backend.model.Organization;
import com.embeddedpayroll.backend.model.PartnerApiClient;
import com.embeddedpayroll.backend.model.WebhookDelivery;
import com.embeddedpayroll.backend.model.WebhookEndpoint;
import com.embeddedpayroll.backend.repository.OrganizationRepository;
import com.embeddedpayroll.backend.repository.PartnerApiClientRepository;
import com.embeddedpayroll.backend.repository.WebhookDeliveryRepository;
import com.embeddedpayroll.backend.repository.WebhookEndpointRepository;
import com.embeddedpayroll.backend.security.ApiKeyHashService;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartnerIntegrationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OrganizationRepository organizationRepository;
    private final PartnerApiClientRepository partnerApiClientRepository;
    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final ApiKeyHashService apiKeyHashService;

    @Transactional(readOnly = true)
    public List<PartnerApiClient> listApiClients(Long organizationId) {
        return partnerApiClientRepository.findByOrganizationIdOrderByDisplayNameAsc(organizationId);
    }

    @Transactional
    public IssuedApiClient createApiClient(IntegrationDtos.CreateApiClientRequest request) {
        Organization organization = organizationRepository.findById(request.organizationId())
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + request.organizationId()));

        String rawApiKey = generateSecret("pk_live_");
        PartnerApiClient partnerApiClient = new PartnerApiClient();
        partnerApiClient.setOrganization(organization);
        partnerApiClient.setClientCode(request.clientCode());
        partnerApiClient.setDisplayName(request.displayName());
        partnerApiClient.setScopes(request.scopes());
        partnerApiClient.setDescription(request.description());
        partnerApiClient.setKeyPrefix(rawApiKey.substring(0, 12));
        partnerApiClient.setApiKeyHash(apiKeyHashService.hash(rawApiKey));
        partnerApiClient.setActive(true);
        return new IssuedApiClient(partnerApiClientRepository.save(partnerApiClient), rawApiKey);
    }

    @Transactional(readOnly = true)
    public List<WebhookEndpoint> listWebhookEndpoints(Long organizationId) {
        return webhookEndpointRepository.findByOrganizationIdOrderByNameAsc(organizationId);
    }

    @Transactional(readOnly = true)
    public List<WebhookDelivery> listWebhookDeliveries(Long organizationId) {
        return webhookDeliveryRepository.findByWebhookEndpointOrganizationIdOrderByCreatedAtDesc(organizationId);
    }

    @Transactional
    public IssuedWebhookEndpoint createWebhook(IntegrationDtos.CreateWebhookRequest request) {
        Organization organization = organizationRepository.findById(request.organizationId())
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + request.organizationId()));

        WebhookEndpoint webhookEndpoint = new WebhookEndpoint();
        webhookEndpoint.setOrganization(organization);
        if (request.partnerApiClientId() != null) {
            PartnerApiClient partnerApiClient = partnerApiClientRepository.findById(request.partnerApiClientId())
                .orElseThrow(() -> new IllegalArgumentException("Partner API client not found: " + request.partnerApiClientId()));
            if (!partnerApiClient.getOrganization().getId().equals(organization.getId())) {
                throw new IllegalArgumentException("Webhook endpoint must belong to the same organization as the partner API client");
            }
            webhookEndpoint.setPartnerApiClient(partnerApiClient);
        }
        webhookEndpoint.setName(request.name());
        webhookEndpoint.setTargetUrl(request.targetUrl());
        webhookEndpoint.setSubscribedEvent(request.subscribedEvent());
        webhookEndpoint.setSigningSecret(generateSecret("whsec_"));
        webhookEndpoint.setActive(true);
        return new IssuedWebhookEndpoint(webhookEndpointRepository.save(webhookEndpoint), webhookEndpoint.getSigningSecret());
    }

    @Transactional(readOnly = true)
    public long countApiClients(Long organizationId) {
        return listApiClients(organizationId).size();
    }

    private String generateSecret(String prefix) {
        byte[] secretBytes = new byte[24];
        SECURE_RANDOM.nextBytes(secretBytes);
        return prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
    }

    public record IssuedApiClient(PartnerApiClient partnerApiClient, String rawApiKey) {
    }

    public record IssuedWebhookEndpoint(WebhookEndpoint webhookEndpoint, String signingSecret) {
    }
}
