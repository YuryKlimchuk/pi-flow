package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitWorktreeCheckTest {

    @TempDir
    Path tempDir;

    private final GitWorktreeCheck check = new GitWorktreeCheck();

    @Test
    void realTempGitRepo_shouldPass() throws Exception {
        Path repo = gitInit(tempDir.resolve("repo"));

        assertThat(check.isGitWorktree(repo)).isTrue();
    }

    @Test
    void emptyRepoWithNoCommits_shouldPass() throws Exception {
        Path repo = gitInit(tempDir.resolve("empty"));

        assertThat(check.isGitWorktree(repo)).isTrue();
    }

    @Test
    void linkedWorktree_shouldPass() throws Exception {
        Path main = gitInit(tempDir.resolve("main"));
        Files.writeString(main.resolve("tracked.txt"), "content");
        git(main, "add", ".");
        git(main, "-c", "user.email=t@example.com", "-c", "user.name=t", "commit", "-m", "init");

        Path linked = tempDir.resolve("linked");
        git(main, "worktree", "add", "-b", "feature", linked.toString());

        assertThat(check.isGitWorktree(linked)).isTrue();
    }

    @Test
    void nonExistentPath_shouldFail() {
        assertThat(check.isGitWorktree(tempDir.resolve("does-not-exist"))).isFalse();
    }

    @Test
    void existingNonGitDirectory_shouldFail() throws Exception {
        Path dir = Files.createDirectories(tempDir.resolve("not-a-repo"));

        assertThat(check.isGitWorktree(dir)).isFalse();
    }

    private static Path gitInit(Path dir) throws Exception {
        Files.createDirectories(dir);
        git(dir, "init");
        return dir;
    }

    private static void git(Path dir, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(dir.toFile());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("git " + String.join(" ", args) + " failed (" + exit + "): " + output);
        }
    }
}
