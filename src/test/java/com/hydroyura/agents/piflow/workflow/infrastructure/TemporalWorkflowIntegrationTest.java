package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.hydroyura.agents.piflow.agent.api.StartAgentCommand;
import com.hydroyura.agents.piflow.github.api.BranchOperations;
import com.hydroyura.agents.piflow.github.api.BranchPublication;
import com.hydroyura.agents.piflow.github.api.PullRequestOperations;
import com.hydroyura.agents.piflow.github.api.PullRequestResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceBranchResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceCommitResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceGitOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceLock;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceLockOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceState;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.PiFlowWorkflow;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.PiFlowWorkflowImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowAnalysisActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowCleanupActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowDesignActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowDeveloperActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowFinalizationActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowInput;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowPreparationActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowReviewerActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowStageState;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemporalWorkflowIntegrationTest {

	private TestWorkflowEnvironment testEnv;

	@TempDir
	Path workspace;

	@Mock
	private WorkspaceOperations workspaceOperations;

	@Mock
	private WorkspaceGitOperations workspaceGitOperations;

	@Mock
	private WorkspaceLockOperations lockOperations;

	@Mock
	private AgentOperations agentOperations;

	@Mock
	private BranchOperations branchOperations;

	@Mock
	private PullRequestOperations pullRequestOperations;

	@BeforeEach
	void setUp() {
		testEnv = TestWorkflowEnvironment.newInstance();
		Worker worker = testEnv.newWorker("pi-flow");
		worker.registerWorkflowImplementationTypes(PiFlowWorkflowImpl.class);
		worker.registerActivitiesImplementations(
				new WorkflowPreparationActivitiesImpl(workspaceOperations, lockOperations),
				new WorkflowAnalysisActivitiesImpl(agentOperations),
				new WorkflowDesignActivitiesImpl(agentOperations),
				new WorkflowDeveloperActivitiesImpl(agentOperations),
				new WorkflowReviewerActivitiesImpl(agentOperations),
				new WorkflowFinalizationActivitiesImpl(workspaceOperations, workspaceGitOperations,
						branchOperations, pullRequestOperations),
				new WorkflowCleanupActivitiesImpl(lockOperations));
		testEnv.start();
	}

	@AfterEach
	void tearDown() {
		testEnv.close();
	}

	@Test
	void fullPath_shouldCompleteWithPrAndReleaseLock() throws IOException, InterruptedException {
		writeArtifacts();
		stubSuccessfulPath();

		WorkflowStub stub = workflowStub();
		stub.start(input());

		awaitStageStatus(stub, "ANALYZING_REQUIREMENTS", "WAITING_FOR_USER");
		stub.signal("continueStage");
		awaitStageStatus(stub, "DESIGNING_SOLUTION", "WAITING_FOR_USER");
		stub.signal("continueStage");
		awaitStageStatus(stub, "REVIEWING", "WAITING_FOR_USER");
		stub.signal("continueStage");

		WorkflowStageState state = awaitStatus(stub, "COMPLETED");

		assertThat(state.status()).isEqualTo("COMPLETED");
		assertThat(state.commitId()).isEqualTo("commit-1");
		assertThat(state.prNumber()).isEqualTo(7);
		assertThat(state.prUrl()).isEqualTo("https://github.com/acme/demo/pull/7");
		verify(workspaceGitOperations).push(any());
		verify(lockOperations).release(any(WorkspaceLock.class));
	}

	@Test
	void buildFailure_afterReworkLimit_shouldFail() throws IOException, InterruptedException {
		writeArtifacts();
		when(workspaceOperations.resolveBaseBranch(any(), any())).thenReturn("main");
		when(workspaceOperations.prepare(any())).thenReturn(preparedWorkspace());
		when(lockOperations.acquire("https://github.com/acme/demo.git", "wf-1"))
				.thenReturn(new WorkspaceLock("key", "wf-1", "token", null));
		when(agentOperations.run(any())).thenAnswer(invocation -> {
			StartAgentCommand command = invocation.getArgument(0);
			if (command.role() == AgentRole.DEVELOPER) {
				return new AgentRunResult("run-dev", AgentRole.DEVELOPER, AgentRunStatus.SUCCEEDED, "", "build failed", 1, Map.of());
			}
			return successResult();
		});

		WorkflowStub stub = workflowStub();
		stub.start(input());

		awaitStageStatus(stub, "ANALYZING_REQUIREMENTS", "WAITING_FOR_USER");
		stub.signal("continueStage");
		awaitStageStatus(stub, "DESIGNING_SOLUTION", "WAITING_FOR_USER");
		stub.signal("continueStage");

		for (int i = 0; i < 4; i++) {
			awaitStageStatus(stub, "IMPLEMENTING", "WAITING_FOR_USER");
			stub.signal("reworkStage", "feedback " + i);
		}

		WorkflowStageState state = awaitStatus(stub, "FAILED");
		assertThat(state.status()).isEqualTo("FAILED");
		assertThat(state.failureReason()).contains("limit exceeded");
	}

	@Test
	void finalizationFailure_shouldFailAndReleaseLock() throws IOException, InterruptedException {
		writeArtifacts();
		stubSuccessfulPath();
		when(pullRequestOperations.createPullRequest(any())).thenThrow(new RuntimeException("github down"));

		WorkflowStub stub = workflowStub();
		stub.start(input());

		awaitStageStatus(stub, "ANALYZING_REQUIREMENTS", "WAITING_FOR_USER");
		stub.signal("continueStage");
		awaitStageStatus(stub, "DESIGNING_SOLUTION", "WAITING_FOR_USER");
		stub.signal("continueStage");
		awaitStageStatus(stub, "REVIEWING", "WAITING_FOR_USER");
		stub.signal("continueStage");

		WorkflowStageState state = awaitStatus(stub, "FAILED");

		assertThat(state.status()).isEqualTo("FAILED");
		verify(lockOperations).release(any(WorkspaceLock.class));
	}

	private void stubSuccessfulPath() {
		when(workspaceOperations.resolveBaseBranch(any(), any())).thenReturn("main");
		when(workspaceOperations.prepare(any())).thenReturn(preparedWorkspace());
		when(lockOperations.acquire("https://github.com/acme/demo.git", "wf-1"))
				.thenReturn(new WorkspaceLock("key", "wf-1", "token", null));
		when(agentOperations.run(any())).thenReturn(successResult());
		when(workspaceGitOperations.commit(any())).thenReturn(
				new WorkspaceCommitResult(workspace, "feature/issue-42", "commit-1"));
		when(workspaceOperations.createBranch(any())).thenReturn(
				new WorkspaceBranchResult(workspace, "feature/issue-42"));
		when(branchOperations.publishBranch("acme", "demo", "feature/issue-42")).thenReturn(
				new BranchPublication("refs/heads/feature/issue-42", "https://github.com/acme/demo/tree/feature/issue-42"));
		when(pullRequestOperations.createPullRequest(any())).thenReturn(
				new PullRequestResult(7, "https://github.com/acme/demo/pull/7"));
	}

	private WorkflowStageState awaitStatus(WorkflowStub stub, String expectedStatus) throws InterruptedException {
		for (int i = 0; i < 200; i++) {
			WorkflowStageState state = stub.query("state", WorkflowStageState.class);
			if (expectedStatus.equals(state.status())) {
				return state;
			}
			Thread.sleep(100);
		}
		return stub.query("state", WorkflowStageState.class);
	}

	private WorkflowStageState awaitStageStatus(WorkflowStub stub, String stage, String status) throws InterruptedException {
		for (int i = 0; i < 200; i++) {
			WorkflowStageState state = stub.query("state", WorkflowStageState.class);
			if (stage.equals(state.stage()) && status.equals(state.status())) {
				return state;
			}
			if ("FAILED".equals(state.status())) {
				return state;
			}
			Thread.sleep(100);
		}
		return stub.query("state", WorkflowStageState.class);
	}

	private WorkflowStub workflowStub() {
		WorkflowClient client = testEnv.getWorkflowClient();
		PiFlowWorkflow workflow = client.newWorkflowStub(PiFlowWorkflow.class,
				WorkflowOptions.newBuilder().setTaskQueue("pi-flow").setWorkflowId("wf-1").build());
		return WorkflowStub.fromTyped(workflow);
	}

	private WorkflowInput input() {
		return new WorkflowInput("wf-1", "https://github.com/acme/demo.git", "acme", "demo", 42, null, null);
	}

	private WorkspaceResult preparedWorkspace() {
		return new WorkspaceResult(workspace, "https://github.com/acme/demo.git", "pi-flow/wf-1", WorkspaceState.CLEAN);
	}

	private AgentRunResult successResult() {
		return new AgentRunResult("run-1", AgentRole.DEVELOPER, AgentRunStatus.SUCCEEDED, "ok", "", 0, Map.of());
	}

	private void writeArtifacts() throws IOException {
		Path analysis = workspace.resolve(".pi-flow/analysis/analyst.md");
		Files.createDirectories(analysis.getParent());
		Files.writeString(analysis, "анализ");
		Files.writeString(workspace.resolve(".pi-flow/analysis/branch.txt"), "feature/issue-42");
		Path design = workspace.resolve(".pi-flow/design/architect.md");
		Files.createDirectories(design.getParent());
		Files.writeString(design, "проект");
	}
}
