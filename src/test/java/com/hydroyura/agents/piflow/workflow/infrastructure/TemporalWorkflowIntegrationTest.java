package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.PiFlowWorkflow;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.PiFlowWorkflowImpl;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowInput;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.WorkflowPreparationActivitiesImpl;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Интеграционный тест Temporal workflow через встроенный test server.
 *
 * <p>Не требует внешнего Docker; использует {@link TestWorkflowEnvironment}.</p>
 */
class TemporalWorkflowIntegrationTest {

	private TestWorkflowEnvironment testEnv;
	private Worker worker;

	@BeforeEach
	void setUp() {
		testEnv = TestWorkflowEnvironment.newInstance();
		worker = testEnv.newWorker("pi-flow");
		worker.registerWorkflowImplementationTypes(PiFlowWorkflowImpl.class);
		worker.registerActivitiesImplementations(new WorkflowPreparationActivitiesImpl());
		testEnv.start();
	}

	@AfterEach
	void tearDown() {
		testEnv.close();
	}

	@Test
	void executesWorkflowAndReturnsPreparedResult() {
		WorkflowClient client = testEnv.getWorkflowClient();
		PiFlowWorkflow workflow = client.newWorkflowStub(
				PiFlowWorkflow.class,
				WorkflowOptions.newBuilder().setTaskQueue("pi-flow").build());

		String result = workflow.execute(new WorkflowInput("wf-1", "https://github.com/o/r", 42));

		assertThat(result).isEqualTo("prepared:wf-1");
	}
}
