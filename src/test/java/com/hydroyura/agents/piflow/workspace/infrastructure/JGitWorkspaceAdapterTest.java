package com.hydroyura.agents.piflow.workspace.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JGitWorkspaceAdapterTest {

	@TempDir
	Path tempDir;

	private JGitWorkspaceAdapter adapter;
	private Path source;
	private String branch;

	@BeforeEach
	void setUp() throws Exception {
		adapter = new JGitWorkspaceAdapter();
		source = Files.createTempDirectory("source-repo");
		try (Git git = Git.init().setDirectory(source.toFile()).call()) {
			Files.writeString(source.resolve("README.md"), "hello");
			git.add().addFilepattern(".").call();
			git.commit().setMessage("init").call();
			branch = git.getRepository().getBranch();
		}
	}

	@Test
	void cloneRepository_whenSourceExists_shouldCreateWorkingCopy() {
		Path target = tempDir.resolve("clone");
		adapter.cloneRepository(source.toUri().toString(), target, branch);

		assertThat(Files.exists(target.resolve("README.md"))).isTrue();
		assertThat(adapter.isRepository(target)).isTrue();
		assertThat(adapter.currentBranch(target)).isEqualTo(branch);
	}

	@Test
	void isRepository_whenEmptyDirectory_shouldReturnFalse() {
		assertThat(adapter.isRepository(tempDir.resolve("empty"))).isFalse();
	}

	@Test
	void isClean_whenModifiedTrackedFile_shouldReturnFalse() throws Exception {
		Path target = tempDir.resolve("clone");
		adapter.cloneRepository(source.toUri().toString(), target, branch);
		Files.writeString(target.resolve("README.md"), "modified");

		assertThat(adapter.isClean(target)).isFalse();
	}

	@Test
	void updateToRemote_whenSourceHasNewCommit_shouldUpdateCopy() throws Exception {
		Path target = tempDir.resolve("clone");
		adapter.cloneRepository(source.toUri().toString(), target, branch);

		try (Git git = Git.open(source.toFile())) {
			Files.writeString(source.resolve("SECOND.md"), "world");
			git.add().addFilepattern(".").call();
			git.commit().setMessage("second").call();
		}

		adapter.updateToRemote(target, branch);

		assertThat(Files.exists(target.resolve("SECOND.md"))).isTrue();
	}

	@Test
	void createBranchAndCheckout_shouldSwitchWorkingCopy() {
		Path target = tempDir.resolve("clone");
		adapter.cloneRepository(source.toUri().toString(), target, branch);

		adapter.createBranch(target, "pi-flow/WF-1", branch);
		adapter.checkout(target, "pi-flow/WF-1");

		assertThat(adapter.branchExists(target, "pi-flow/WF-1")).isTrue();
		assertThat(adapter.currentBranch(target)).isEqualTo("pi-flow/WF-1");
	}

	@Test
	void commit_whenNewFile_shouldCreateCommit() throws Exception {
		Path target = tempDir.resolve("clone");
		adapter.cloneRepository(source.toUri().toString(), target, branch);
		Files.writeString(target.resolve("NEW.md"), "new file");

		String commitId = adapter.commit(target, "add new file", List.of("NEW.md"));

		assertThat(commitId).isNotBlank();
		assertThat(adapter.isClean(target)).isTrue();
	}
}
