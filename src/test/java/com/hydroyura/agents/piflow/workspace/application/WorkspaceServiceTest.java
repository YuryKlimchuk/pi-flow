package com.hydroyura.agents.piflow.workspace.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceArtifactResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceException;
import com.hydroyura.agents.piflow.workspace.api.WorkspacePreparationRequest;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceState;
import com.hydroyura.agents.piflow.workspace.domain.LocalGit;
import com.hydroyura.agents.piflow.workspace.domain.WorkspaceGitException;
import com.hydroyura.agents.piflow.workspace.domain.WorkspaceLayout;
import com.hydroyura.agents.piflow.workspace.infrastructure.PropertiesWorkspaceLayout;
import com.hydroyura.agents.piflow.workspace.infrastructure.WorkspaceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

	@TempDir
	Path tempDir;

	@Mock
	private LocalGit localGit;

	private WorkspaceService service;

	@BeforeEach
	void setUp() {
		WorkspaceLayout layout = new PropertiesWorkspaceLayout(
				new WorkspaceProperties(tempDir.resolve("ws"), tempDir.resolve("locks"), Duration.ofMinutes(1)));
		service = new WorkspaceService(localGit, layout);
		lenient().when(localGit.resolveBaseBranch(any(), any())).thenAnswer(inv -> inv.getArgument(1));
	}

	@Test
	void prepare_whenNewRepository_shouldCloneAndCheckoutWorkingBranch() {
		var request = new WorkspacePreparationRequest("WF-1", "https://github.com/acme/repo.git", "main", Map.of());
		when(localGit.branchExists(tempDir.resolve("ws/github.com-acme-repo"), "pi-flow/WF-1")).thenReturn(false);
		when(localGit.isClean(tempDir.resolve("ws/github.com-acme-repo"))).thenReturn(true);

		WorkspaceResult result = service.prepare(request);

		assertThat(result.branch()).isEqualTo("pi-flow/WF-1");
		assertThat(result.state()).isEqualTo(WorkspaceState.CLEAN);
		verify(localGit).cloneRepository("https://github.com/acme/repo.git", tempDir.resolve("ws/github.com-acme-repo"), "main");
		verify(localGit).createBranch(tempDir.resolve("ws/github.com-acme-repo"), "pi-flow/WF-1", "main");
		verify(localGit).checkout(tempDir.resolve("ws/github.com-acme-repo"), "pi-flow/WF-1");
	}

	@Test
	void prepare_whenExistingRepositoryIsDirty_shouldThrowDirty() throws Exception {
		Path dir = tempDir.resolve("ws/github.com-acme-repo");
		Files.createDirectories(dir);
		var request = new WorkspacePreparationRequest("WF-1", "https://github.com/acme/repo.git", "main", Map.of());
		when(localGit.isRepository(dir)).thenReturn(true);
		when(localGit.isClean(dir)).thenReturn(false);

		WorkspaceException error = catchThrowableOfType(() -> service.prepare(request), WorkspaceException.class);

		assertThat(error.errorType()).isEqualTo(WorkspaceException.ErrorType.DIRTY);
	}

	@Test
	void prepare_whenDirectoryIsNotRepository_shouldThrowCorrupt() throws Exception {
		Path dir = tempDir.resolve("ws/github.com-acme-repo");
		Files.createDirectories(dir);
		var request = new WorkspacePreparationRequest("WF-1", "https://github.com/acme/repo.git", "main", Map.of());
		when(localGit.isRepository(dir)).thenReturn(false);

		WorkspaceException error = catchThrowableOfType(() -> service.prepare(request), WorkspaceException.class);

		assertThat(error.errorType()).isEqualTo(WorkspaceException.ErrorType.CORRUPT);
	}

	@Test
	void prepare_whenGitError_shouldMapToGit() {
		var request = new WorkspacePreparationRequest("WF-1", "https://github.com/acme/repo.git", "main", Map.of());
		Path dir = tempDir.resolve("ws/github.com-acme-repo");
		when(localGit.branchExists(dir, "pi-flow/WF-1")).thenThrow(
				WorkspaceGitException.git("boom", new IllegalStateException()));

		WorkspaceException error = catchThrowableOfType(() -> service.prepare(request), WorkspaceException.class);

		assertThat(error.errorType()).isEqualTo(WorkspaceException.ErrorType.GIT);
	}

	@Test
	void readAnalysisArtifact_whenExists_shouldReturnText() throws IOException {
		Path ws = tempDir.resolve("ws1");
		Path artifact = ws.resolve(".pi-flow/analysis/analyst.md");
		Files.createDirectories(artifact.getParent());
		Files.writeString(artifact, "текст анализа");

		WorkspaceArtifactResult result = service.readAnalysisArtifact(ws.toString());

		assertThat(result.text()).isEqualTo("текст анализа");
		assertThat(result.path()).isEqualTo(artifact.toString());
	}

	@Test
	void readAnalysisArtifact_whenMissing_shouldThrowResult() throws IOException {
		Path ws = tempDir.resolve("ws2");
		Files.createDirectories(ws);

		WorkspaceException error = catchThrowableOfType(() -> service.readAnalysisArtifact(ws.toString()),
				WorkspaceException.class);

		assertThat(error.errorType()).isEqualTo(WorkspaceException.ErrorType.RESULT);
	}

	@Test
	void readAnalysisArtifact_whenEmpty_shouldThrowResult() throws IOException {
		Path ws = tempDir.resolve("ws3");
		Path artifact = ws.resolve(".pi-flow/analysis/analyst.md");
		Files.createDirectories(artifact.getParent());
		Files.writeString(artifact, "   ");

		WorkspaceException error = catchThrowableOfType(() -> service.readAnalysisArtifact(ws.toString()),
				WorkspaceException.class);

		assertThat(error.errorType()).isEqualTo(WorkspaceException.ErrorType.RESULT);
	}

	@Test
	void readAnalysisArtifact_whenBlankPath_shouldThrowValidation() {
		WorkspaceException error = catchThrowableOfType(() -> service.readAnalysisArtifact("  "),
				WorkspaceException.class);

		assertThat(error.errorType()).isEqualTo(WorkspaceException.ErrorType.VALIDATION);
	}
}
