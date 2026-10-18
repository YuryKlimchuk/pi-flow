package com.hydroyura.agents.piflow.workflow.application;

/** The category of a change-identifier resolution attempt. */
public enum ChangeResolutionStatus {
    RESOLVED,
    NOT_FOUND,
    AMBIGUOUS,
    PATH_ESCAPING,
    CLI_FAILURE
}
