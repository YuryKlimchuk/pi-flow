package com.hydroyura.agents.piflow.workflow.application;

/** The four-valued result of a deterministic verification attempt. */
public enum VerificationStatus {
    PASSED,
    FAILED,
    NOT_CONFIGURED,
    INFRASTRUCTURE_ERROR
}
