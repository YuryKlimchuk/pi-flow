package com.hydroyura.agents.piflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import com.hydroyura.agents.piflow.workspace.application.WorkspaceService;
import com.hydroyura.agents.piflow.workspace.infrastructure.JGitWorkspaceAdapter;
import com.hydroyura.agents.piflow.workspace.infrastructure.PropertiesWorkspaceLayout;
import com.hydroyura.agents.piflow.workspace.infrastructure.WorkspaceProperties;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowInput;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowPreparationActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowPreparationResult;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Кросс-модульный интеграционный тест: activity подготовки workspace из
 * {@code workflow} вызывает реальный {@code workspace} adapter (JGit) и
 * проверяет фактическое клонирование репозитория и создание рабочей ветки
 * {@code pi-flow/<workflowId>}.
 *
 * <p>Размещён в корневом пакете, так как связывает {@code workspace.infrastructure}
 * с {@code workflow.infrastructure.temporal} — это запрещено внутри модулей.</p>
 */
class WorkflowPreparationActivityIntegrationTest {

	@TempDir
	Path tempDir;

	private WorkflowPreparationActivitiesImpl activity;
	private String sourceUrl;

	@BeforeEach
	void setUp() throws Exception {
		Path source = tempDir.resolve("source");
		Files.createDirectories(source);
		try (Git git = Git.init().setDirectory(source.toFile()).call()) {
			Files.writeString(source.resolve("README.md"), "hello");
			git.add().addFilepattern(".").call();
			git.commit().setMessage("init").call();
			String defaultBranch = git.getRepository().getBranch();
			if (!"main".equals(defaultBranch)) {
				git.branchRename().setOldName(defaultBranch).setNewName("main").call();
			}
		}
		sourceUrl = source.toUri().toString();

		Path base = tempDir.resolve("ws");
		Path lock = tempDir.resolve("locks");
		WorkspaceService workspaceService = new WorkspaceService(
				new JGitWorkspaceAdapter(),
				new PropertiesWorkspaceLayout(new WorkspaceProperties(base, lock, Duration.ofMinutes(1))));
		activity = new WorkflowPreparationActivitiesImpl(workspaceService);
	}

	@Test
	void prepare_shouldCloneAndCreateWorkingBranch() {
		WorkflowInput input = new WorkflowInput("WF-1", sourceUrl, "local", "source", 42);

		WorkflowPreparationResult result = activity.prepare(input);

		assertThat(result.branch()).isEqualTo("pi-flow/WF-1");
		assertThat(result.path()).isNotBlank();
		assertThat(Files.exists(Path.of(result.path()).resolve("README.md"))).isTrue();
	}

	@Test
	void prepare_whenSourceUnavailable_shouldPropagateWorkspaceError() {
		WorkflowInput input = new WorkflowInput("WF-2", "file:///nonexistent/repo.git", "local", "source", 42);

		assertThatThrownBy(() -> activity.prepare(input))
				.isInstanceOf(com.hydroyura.agents.piflow.workspace.api.WorkspaceException.class);
	}
}
