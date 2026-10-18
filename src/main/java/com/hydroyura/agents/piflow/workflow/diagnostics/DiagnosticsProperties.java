package com.hydroyura.agents.piflow.workflow.diagnostics;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Diagnostics bounds, bound from {@code pi-flow.diagnostics.*}.
 *
 * @param maxEntriesPerWorkflow maximum number of diagnostic entries retained per workflow
 *                              (default 200)
 * @param maxReasonLength       maximum length of each entry's reason string (default 500)
 */
@ConfigurationProperties(prefix = "pi-flow.diagnostics")
public record DiagnosticsProperties(Integer maxEntriesPerWorkflow, Integer maxReasonLength) {

    public DiagnosticsProperties {
        if (maxEntriesPerWorkflow == null || maxEntriesPerWorkflow < 1) {
            maxEntriesPerWorkflow = 200;
        }
        if (maxReasonLength == null || maxReasonLength < 1) {
            maxReasonLength = 500;
        }
    }
}
