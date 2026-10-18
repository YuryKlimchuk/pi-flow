package com.hydroyura.agents.piflow.workflow.application;

/** Port for resolving a candidate change identifier against the target repository's OpenSpec changes. */
public interface ChangeResolver {

    /**
     * Resolves {@code changeId} against the OpenSpec CLI's view of the target repository's active changes.
     *
     * <p>Rejects path-escaping identifiers before invoking the CLI, and reports zero/ambiguous matches,
     * CLI invocation failures, and unparseable output as distinct, non-resolved categories.
     */
    ChangeResolution resolve(String targetRepo, String changeId);
}
