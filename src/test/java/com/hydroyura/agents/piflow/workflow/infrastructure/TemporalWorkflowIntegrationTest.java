package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceResult;
import com.hydroyura.agents.piflow.workspace.api.WorkspaceState;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.PiFlowWorkflow;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.PiFlowWorkflowImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowInput;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowPreparationActivitiesImpl;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Интеграционный тест Temporal workflow через встроенный test server.
 *
 * <p>Не требует внешнего Docker; использует {@link TestWorkflowEnvironment}.
 * Подготовка workspace в activity замокана.</p>
 */
@ExtendWith(MockitoExtension.class)
class TemporalWorkflowIntegrationTest {

	private TestWorkflowEnvironment testEnv;

	@Mock
	private WorkspaceOperations workspaceOperations;

	@BeforeEach
	void setUp() {
		testEnv = TestWorkflowEnvironment.newInstance();
		Worker worker = testEnv.newWorker("pi-flow");
		worker.registerWorkflowImplementationTypes(PiFlowWorkflowImpl.class);
		worker.registerActivitiesImplementations(new WorkflowPreparationActivitiesImpl(workspaceOperations));
		testEnv.start();
	}

	@AfterEach
	void tearDown() {
		testEnv.close();
	}

	@Test
	void executesWorkflowAndReturnsPreparedBranch() {
		when(workspaceOperations.prepare(any())).thenReturn(
				new WorkspaceResult(Path.of("/tmp/ws"), "https://github.com/acme/demo.git", "pi-flow/wf-1", WorkspaceState.CLEAN));

		WorkflowClient client = testEnv.getWorkflowClient();
		PiFlowWorkflow workflow = client.newWorkflowStub(
				PiFlowWorkflow.class,
				WorkflowOptions.newBuilder().setTaskQueue("pi-flow").build());

		String result = workflow.execute(new WorkflowInput("wf-1", "https://github.com/acme/demo.git", "acme", "demo", 42));

		assertThat(result).isEqualTo("pi-flow/wf-1");
	}
}
