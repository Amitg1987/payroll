package com.embeddedpayroll.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.webhooks", name = "dispatch-enabled", havingValue = "true", matchIfMissing = true)
public class WebhookDispatchScheduler {

    private final WebhookService webhookService;

    @Scheduled(fixedDelayString = "${app.webhooks.dispatch-interval-ms:60000}")
    public void dispatchPending() {
        webhookService.dispatchPendingDeliveries();
    }
}
