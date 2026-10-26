package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.hydroyura.agents.piflow.agent.api.AgentOperations;
import com.hydroyura.agents.piflow.agent.api.AgentRole;
import com.hydroyura.agents.piflow.agent.api.AgentRunResult;
import com.hydroyura.agents.piflow.agent.api.AgentRunStatus;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceState;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.PiFlowWorkflow;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.PiFlowWorkflowImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowAnalysisActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowAnalysisResult;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowInput;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowPreparationActivitiesImpl;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Интеграционный тест Temporal workflow через встроенный test server.
 *
 * <p>Не требует внешнего Docker; использует {@link TestWorkflowEnvironment}.
 * Подготовка workspace и запуск агента в activities замоканы.</p>
 */
@ExtendWith(MockitoExtension.class)
class TemporalWorkflowIntegrationTest {

	private static final String ARTIFACT = ".pi-flow/analysis/analyst.md";
	private static final String BRANCH = ".pi-flow/analysis/branch.txt";

	private TestWorkflowEnvironment testEnv;

	@TempDir
	Path workspace;

	@Mock
	private WorkspaceOperations workspaceOperations;

	@Mock
	private AgentOperations agentOperations;

	@BeforeEach
	void setUp() {
		testEnv = TestWorkflowEnvironment.newInstance();
		Worker worker = testEnv.newWorker("pi-flow");
		worker.registerWorkflowImplementationTypes(PiFlowWorkflowImpl.class);
		worker.registerActivitiesImplementations(
				new WorkflowPreparationActivitiesImpl(workspaceOperations),
				new WorkflowAnalysisActivitiesImpl(agentOperations));
		testEnv.start();
	}

	@AfterEach
	void tearDown() {
		testEnv.close();
	}

	@Test
	void executesWorkflowAndReturnsAnalysisArtifactPath() throws IOException {
		writeArtifact("анализ");
		when(workspaceOperations.prepare(any())).thenReturn(preparedWorkspace());
		when(agentOperations.run(any())).thenReturn(successResult());

		WorkflowAnalysisResult result = workflowStub().execute(input());

		assertThat(result.workspacePath()).isEqualTo(workspace.toString());
		assertThat(result.branchName()).isEqualTo("feature/issue-42");
	}

	@Test
	void preparationFailure_shouldNotInvokeAnalyst() {
		when(workspaceOperations.prepare(any())).thenThrow(new RuntimeException("prepare failed"));

		assertThatThrownBy(() -> workflowStub().execute(input())).isInstanceOf(WorkflowFailedException.class);

		verify(agentOperations, never()).run(any());
	}

	@Test
	void analysisFailure_shouldFailWorkflow() {
		when(workspaceOperations.prepare(any())).thenReturn(preparedWorkspace());
		when(agentOperations.run(any())).thenReturn(
				new AgentRunResult("run-1", AgentRole.ANALYST, AgentRunStatus.FAILED, "", "err", 1, Map.of()));

		assertThatThrownBy(() -> workflowStub().execute(input())).isInstanceOf(WorkflowFailedException.class);
	}

	@Test
	void invalidArtifact_shouldFailWorkflow() {
		when(workspaceOperations.prepare(any())).thenReturn(preparedWorkspace());
		when(agentOperations.run(any())).thenReturn(successResult());

		assertThatThrownBy(() -> workflowStub().execute(input())).isInstanceOf(WorkflowFailedException.class);
	}

	private PiFlowWorkflow workflowStub() {
		WorkflowClient client = testEnv.getWorkflowClient();
		return client.newWorkflowStub(PiFlowWorkflow.class,
				WorkflowOptions.newBuilder().setTaskQueue("pi-flow").build());
	}

	private WorkflowInput input() {
		return new WorkflowInput("wf-1", "https://github.com/acme/demo.git", "acme", "demo", 42);
	}

	private WorkspaceResult preparedWorkspace() {
		return new WorkspaceResult(workspace, "https://github.com/acme/demo.git", "pi-flow/wf-1", WorkspaceState.CLEAN);
	}

	private AgentRunResult successResult() {
		return new AgentRunResult("run-1", AgentRole.ANALYST, AgentRunStatus.SUCCEEDED, "out", "", 0, Map.of());
	}

	private void writeArtifact(String content) throws IOException {
		Path artifact = workspace.resolve(ARTIFACT);
		Files.createDirectories(artifact.getParent());
		Files.writeString(artifact, content);
		Files.writeString(workspace.resolve(BRANCH), "feature/issue-42");
	}
}
