package com.hydroyura.agents.piflow.workflow.application;

import java.util.Objects;

/**
 * Result of a deterministic strict-validation attempt over a bound change's artifacts.
 *
 * @param passed whether strict validation passed
 * @param reason the captured rejection reason when validation did not pass, empty when it passed
 */
public record ValidationResult(boolean passed, String reason) {

    public ValidationResult {
        reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, "");
    }

    public static ValidationResult failed(String reason) {
        return new ValidationResult(false, Objects.requireNonNull(reason, "reason must not be null"));
    }
}
