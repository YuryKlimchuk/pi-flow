package com.hydroyura.agents.piflow.workflow.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Verification configuration, bound from {@code pi-flow.verification.*}.
 *
 * <p>This is the only startup-bound verification setting and it configures <em>where to look</em>
 * for the per-target-repository configuration file, never <em>what command to run</em>. The command,
 * timeout, and working directory are read fresh from that file on every verification attempt.
 *
 * @param configPath path to the verification configuration file, relative to the target repository
 *                   root unless absolute
 */
@ConfigurationProperties(prefix = "pi-flow.verification")
public record VerificationProperties(String configPath) {

    public VerificationProperties {
        if (configPath == null || configPath.isBlank()) {
            configPath = ".pi-flow/verify.json";
        }
    }
}
