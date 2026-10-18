package com.hydroyura.agents.piflow.workflow.application;

import java.util.Objects;
import java.util.Optional;

/**
 * Typed context the orchestrator builds immediately before each agent stage invocation.
 *
 * @param task             workflow task description
 * @param targetRepo       target repository path
 * @param analysisFindings recorded {@code PROJECT_ANALYSIS} output, empty when that stage has not run
 * @param proposalOutput   recorded {@code CHANGE_PROPOSAL} output, empty when that stage has not run
 * @param pendingFeedback  user-supplied revision/further-work feedback, empty when none is pending
 */
public record StageContext(
        String task,
        String targetRepo,
        Optional<String> analysisFindings,
        Optional<String> proposalOutput,
        Optional<String> pendingFeedback) {

    public StageContext {
        task = Objects.requireNonNull(task, "task must not be null");
        targetRepo = Objects.requireNonNull(targetRepo, "targetRepo must not be null");
        analysisFindings = Objects.requireNonNull(analysisFindings, "analysisFindings must not be null");
        proposalOutput = Objects.requireNonNull(proposalOutput, "proposalOutput must not be null");
        pendingFeedback = Objects.requireNonNull(pendingFeedback, "pendingFeedback must not be null");
    }
}
