package com.hydroyura.agents.piflow.workflow.application;

/** Port for running deterministic strict validation of a bound change's artifacts. */
public interface ChangeValidator {

    /**
     * Runs strict validation of {@code changeId} in {@code targetRepo}, reporting pass/fail
     * with the captured rejection reason on failure.
     */
    ValidationResult validate(String targetRepo, String changeId);
}
