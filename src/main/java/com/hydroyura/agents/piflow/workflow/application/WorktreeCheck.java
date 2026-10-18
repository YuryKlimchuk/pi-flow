package com.hydroyura.agents.piflow.workflow.application;

import java.nio.file.Path;

/** Port for resolving whether a path is a canonical Git worktree root. */
public interface WorktreeCheck {

    /**
     * Reports whether the given path resolves to a Git worktree root via {@code git rev-parse},
     * accepting main and linked worktrees and repositories with no commits yet.
     */
    boolean isGitWorktree(Path targetRepo);
}
