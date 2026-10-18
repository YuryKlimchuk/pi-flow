package com.hydroyura.agents.piflow.workflow.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bounded, append-only, in-memory reporting projection of a workflow's stage attempts.
 *
 * <p>This is a derived, truncatable reporting record, distinct from plan 15's full, unbounded
 * {@code Workflow.outcomes()} history. It is retained only in process memory for the lifetime of
 * the owning {@code Workflow} and is never consulted for retry or budget decisions.
 */
public final class WorkflowDiagnostics {

    private final int maxEntries;
    private final List<DiagnosticEntry> entries = new ArrayList<>();
    private boolean entriesDropped;

    public WorkflowDiagnostics(DiagnosticsProperties properties) {
        Objects.requireNonNull(properties, "properties must not be null");
        this.maxEntries = properties.maxEntriesPerWorkflow();
    }

    /** Appends an entry, dropping the oldest entries once the per-workflow cap is exceeded. */
    public void record(DiagnosticEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        entries.add(entry);
        while (entries.size() > maxEntries) {
            entries.removeFirst();
            entriesDropped = true;
        }
    }

    /** The retained diagnostic entries, in chronological order (oldest first). */
    public List<DiagnosticEntry> entries() {
        return List.copyOf(entries);
    }

    /** Whether any entries were dropped after the per-workflow cap was exceeded (sticky). */
    public boolean entriesDropped() {
        return entriesDropped;
    }
}
