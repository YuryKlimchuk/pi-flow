package com.hydroyura.agents.piflow.workflow.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed-конфигурация подключения к Temporal.
 *
 * @param endpoint gRPC endpoint Temporal
 * @param namespace namespace Temporal
 * @param taskQueue task queue исполнения workflow
 */
@ConfigurationProperties(prefix = "workflow.temporal")
public record TemporalProperties(
		@DefaultValue("localhost:7233") String endpoint,
		@DefaultValue("default") String namespace,
		@DefaultValue("pi-flow") String taskQueue) {
}
