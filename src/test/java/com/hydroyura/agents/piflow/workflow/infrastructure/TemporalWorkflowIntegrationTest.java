package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowDesignActivitiesImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowInput;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowPreparationActivitiesImpl;
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
	private AgentOperations agentOperations;

	@BeforeEach
	void setUp() {
		testEnv = TestWorkflowEnvironment.newInstance();
		Worker worker = testEnv.newWorker("pi-flow");
		worker.registerWorkflowImplementationTypes(PiFlowWorkflowImpl.class);
		worker.registerActivitiesImplementations(
				new WorkflowPreparationActivitiesImpl(workspaceOperations),
				new WorkflowAnalysisActivitiesImpl(agentOperations),
				new WorkflowDesignActivitiesImpl(agentOperations));
		testEnv.start();
	}

	@AfterEach
	void tearDown() {
		testEnv.close();
	}

	@Test
	void longWorkflow_reachesAnalystGate() throws IOException, InterruptedException {
		writeAnalysisArtifacts();
		when(workspaceOperations.prepare(any())).thenReturn(preparedWorkspace());
		when(agentOperations.run(any())).thenReturn(successResult());

		WorkflowStub stub = workflowStub();
		stub.start(input());

		WorkflowStageState state = awaitStatus(stub, "WAITING_FOR_USER");

		assertThat(state.stage()).isEqualTo("ANALYZING_REQUIREMENTS");
		assertThat(state.workspacePath()).isEqualTo(workspace.toString());
	}

	private WorkflowStageState awaitStatus(WorkflowStub stub, String expectedStatus) throws InterruptedException {
		for (int i = 0; i < 100; i++) {
			WorkflowStageState state = stub.query("state", WorkflowStageState.class);
			if (expectedStatus.equals(state.status())) {
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
		return new AgentRunResult("run-1", AgentRole.ANALYST, AgentRunStatus.SUCCEEDED, "out", "", 0, Map.of());
	}

	private void writeAnalysisArtifacts() throws IOException {
		Path artifact = workspace.resolve(".pi-flow/analysis/analyst.md");
		Files.createDirectories(artifact.getParent());
		Files.writeString(artifact, "анализ");
		Files.writeString(workspace.resolve(".pi-flow/analysis/branch.txt"), "feature/issue-42");
	}
}
