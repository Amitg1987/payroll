package com.embeddedpayroll.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.temporal")
public record TemporalProperties(
    boolean enabled,
    String namespace,
    String target,
    String taskQueue
) {
}
