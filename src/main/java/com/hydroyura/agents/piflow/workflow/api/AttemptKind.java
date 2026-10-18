package com.hydroyura.agents.piflow.workflow.api;

/**
 * Classification of a single stage attempt.
 *
 * <p>Every recorded {@link StageOutcome} carries exactly one of these kinds:
 * <ul>
 *   <li>{@link #INITIAL} — the stage's first attempt in its current attempt sequence.</li>
 *   <li>{@link #TECHNICAL_RETRY} — a re-run of a stage whose prior execution did not complete
 *       validly (process crash, timeout, or malformed/unparseable output).</li>
 *   <li>{@link #CONTENT_CORRECTION} — a validly-executed stage whose result required another pass
 *       (a {@code REVIEW} {@code CHANGES_REQUIRED} verdict, or a verification command that ran to
 *       completion and reported a failing exit code).</li>
 * </ul>
 */
public enum AttemptKind {
    INITIAL,
    TECHNICAL_RETRY,
    CONTENT_CORRECTION
}
