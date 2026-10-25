package com.hydroyura.agents.piflow.workflow.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Проверяет создание, состояние и историю агрегата Workflow.
 */
class WorkflowDomainTest {

	@Test
	void createsWorkflowInCreatedStateOnAnalyzingStage() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", 42));

		assertThat(workflow.id().value()).isEqualTo("wf-1");
		assertThat(workflow.status()).isEqualTo(WorkflowStatus.CREATED);
		assertThat(workflow.currentStage()).isEqualTo(WorkflowStage.ANALYZING_REQUIREMENTS);
		assertThat(workflow.completedStages()).isEmpty();
		assertThat(workflow.checkpoints()).isEmpty();
		assertThat(workflow.executionId()).isNull();
	}

	@Test
	void rejectsBlankWorkflowId() {
		assertThatThrownBy(() -> new WorkflowId(" "))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsNonPositiveIssueNumber() {
		assertThatThrownBy(() -> new WorkflowContext("https://github.com/o/r", 0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void completesAnalyzingRequirementsToDesigningSolution() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", 42));

		WorkflowStage next = workflow.completeStage();

		assertThat(next).isEqualTo(WorkflowStage.DESIGNING_SOLUTION);
		assertThat(workflow.status()).isEqualTo(WorkflowStatus.RUNNING);
		assertThat(workflow.completedStages()).containsExactly(WorkflowStage.ANALYZING_REQUIREMENTS);
	}

	@Test
	void rejectsIllegalTransitionWithoutChangingState() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", 42));

		assertThatThrownBy(() -> workflow.completeStage(WorkflowStage.REVIEWING))
				.isInstanceOf(IllegalWorkflowTransitionException.class);

		assertThat(workflow.status()).isEqualTo(WorkflowStatus.CREATED);
		assertThat(workflow.currentStage()).isEqualTo(WorkflowStage.ANALYZING_REQUIREMENTS);
		assertThat(workflow.completedStages()).isEmpty();
	}

	@Test
	void attachesExecutionAndRecordsCheckpoint() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", 42));

		workflow.attachExecution("exec-1");

		assertThat(workflow.executionId()).isEqualTo("exec-1");
		assertThat(workflow.checkpoints()).contains(new Checkpoint("execution", "exec-1"));
	}
}
