package com.hydroyura.agents.piflow.workflow.infrastructure;

import com.hydroyura.agents.piflow.workflow.application.WorktreeCheck;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Resolves whether a path is a canonical Git worktree root via {@code git rev-parse}.
 *
 * <p>Performs no Git write operation; a dirty worktree and an empty (no-commit) repository are accepted.
 */
@Component
public class GitWorktreeCheck implements WorktreeCheck {

    private final String command;
    private final Duration timeout;

    public GitWorktreeCheck() {
        this("git", Duration.ofSeconds(30));
    }

    public GitWorktreeCheck(String command, Duration timeout) {
        this.command = Objects.requireNonNull(command, "command must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
    }

    @Override
    public boolean isGitWorktree(Path targetRepo) {
        Objects.requireNonNull(targetRepo, "targetRepo must not be null");
        ExternalCommand.Result result = ExternalCommand.run(
                List.of(command, "-C", targetRepo.toString(), "rev-parse", "--show-toplevel"),
                targetRepo, timeout);
        return !result.startFailed() && !result.timedOut() && result.exitCode() == 0;
    }
}
