package com.hydroyura.agents.piflow.workflow.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Retry and correction budgets, bound from {@code pi-flow.retry.*}.
 *
 * @param maxTechnicalRetries    maximum number of technical-failure attempts per stage-attempt-sequence
 *                               (default 3)
 * @param maxContentCorrections  maximum number of content-correction loops per workflow (default 5)
 */
@ConfigurationProperties(prefix = "pi-flow.retry")
public record RetryProperties(Integer maxTechnicalRetries, Integer maxContentCorrections) {

    public RetryProperties {
        if (maxTechnicalRetries == null || maxTechnicalRetries < 0) {
            maxTechnicalRetries = 3;
        }
        if (maxContentCorrections == null || maxContentCorrections < 0) {
            maxContentCorrections = 5;
        }
    }
}
