package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.model.WebhookDelivery;
import com.embeddedpayroll.backend.model.WebhookEndpoint;
import com.embeddedpayroll.backend.repository.WebhookDeliveryRepository;
import com.embeddedpayroll.backend.repository.WebhookEndpointRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebhookService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueueEvent(Long organizationId, String eventType, String eventKey, Object payload) {
        List<WebhookEndpoint> endpoints = webhookEndpointRepository.findByOrganizationIdAndActiveTrue(organizationId)
            .stream()
            .filter(endpoint -> endpoint.getSubscribedEvent().equals("*") || endpoint.getSubscribedEvent().equals(eventType))
            .toList();

        if (endpoints.isEmpty()) {
            return;
        }

        String payloadJson = writePayload(payload);
        for (WebhookEndpoint endpoint : endpoints) {
            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setWebhookEndpoint(endpoint);
            delivery.setEventType(eventType);
            delivery.setEventKey(eventKey);
            delivery.setPayloadJson(payloadJson);
            delivery.setSignature(signPayload(endpoint.getSigningSecret(), payloadJson));
            delivery.setDeliveryStatus(WebhookDelivery.DeliveryStatus.PENDING);
            delivery.setAttempts(0);
            delivery.setNextAttemptAt(OffsetDateTime.now());
            webhookDeliveryRepository.save(delivery);
        }
    }

    @Transactional(readOnly = true)
    public long countPendingDeliveries(Long organizationId) {
        return webhookDeliveryRepository.countByWebhookEndpointOrganizationIdAndDeliveryStatus(
            organizationId,
            WebhookDelivery.DeliveryStatus.PENDING
        );
    }

    @Transactional
    public int dispatchPendingDeliveries() {
        List<WebhookDelivery> pendingDeliveries = webhookDeliveryRepository
            .findTop20ByDeliveryStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                List.of(WebhookDelivery.DeliveryStatus.PENDING, WebhookDelivery.DeliveryStatus.FAILED),
                OffsetDateTime.now()
            );

        for (WebhookDelivery delivery : pendingDeliveries) {
            send(delivery);
        }
        return pendingDeliveries.size();
    }

    private void send(WebhookDelivery delivery) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(delivery.getWebhookEndpoint().getTargetUrl()))
                .header("Content-Type", "application/json")
                .header("X-Webhook-Event", delivery.getEventType())
                .header("X-Webhook-Event-Key", delivery.getEventKey())
                .header("X-Webhook-Signature", delivery.getSignature())
                .POST(HttpRequest.BodyPublishers.ofString(delivery.getPayloadJson()))
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            delivery.setAttempts(delivery.getAttempts() + 1);
            delivery.setResponseStatusCode(response.statusCode());
            delivery.setResponseBody(response.body());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                delivery.setDeliveryStatus(WebhookDelivery.DeliveryStatus.SENT);
                delivery.setDeliveredAt(OffsetDateTime.now());
                delivery.setNextAttemptAt(null);
            } else {
                markFailed(delivery);
            }
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            delivery.setResponseBody(exception.getMessage());
            delivery.setAttempts(delivery.getAttempts() + 1);
            markFailed(delivery);
        }
    }

    private void markFailed(WebhookDelivery delivery) {
        if (delivery.getAttempts() >= 5) {
            delivery.setDeliveryStatus(WebhookDelivery.DeliveryStatus.DEAD);
            delivery.setNextAttemptAt(null);
            return;
        }
        delivery.setDeliveryStatus(WebhookDelivery.DeliveryStatus.FAILED);
        delivery.setNextAttemptAt(OffsetDateTime.now().plusMinutes(Math.max(1, delivery.getAttempts() * 5L)));
    }

    private String signPayload(String secret, String payloadJson) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign webhook payload", exception);
        }
    }

    private String writePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize webhook payload", exception);
        }
    }
}
