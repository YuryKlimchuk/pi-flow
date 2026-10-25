package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.hydroyura.agents.piflow.workflow.api.WorkflowException;
import com.hydroyura.agents.piflow.workflow.application.WorkflowExecutionCommand;
import com.hydroyura.agents.piflow.workflow.infrastructure.temporal.PiFlowWorkflow;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemporalWorkflowAdapterTest {

	@Mock
	private WorkflowClient workflowClient;

	@Test
	void start_whenTemporalUnavailable_shouldTranslateToWorkflowException() {
		TemporalProperties properties = new TemporalProperties("localhost:7233", "default", "pi-flow");
		TemporalWorkflowAdapter adapter = new TemporalWorkflowAdapter(workflowClient, properties);

		when(workflowClient.newWorkflowStub(any(Class.class), any(WorkflowOptions.class)))
				.thenThrow(new RuntimeException("connection refused"));

		WorkflowException error = catchThrowableOfType(
				() -> adapter.start(new WorkflowExecutionCommand("wf-1", "https://github.com/o/r", 42)),
				WorkflowException.class);

		assertThat(error.errorType()).isEqualTo(WorkflowException.ErrorType.EXECUTION);
	}
}
