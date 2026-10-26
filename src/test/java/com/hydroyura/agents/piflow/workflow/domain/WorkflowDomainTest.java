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
				new WorkflowContext("https://github.com/o/r", "o", "r", 42));

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
		assertThatThrownBy(() -> new WorkflowContext("https://github.com/o/r", "o", "r", 0))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void completesAnalyzingRequirementsToDesigningSolution() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", "o", "r", 42));

		WorkflowStage next = workflow.completeStage();

		assertThat(next).isEqualTo(WorkflowStage.DESIGNING_SOLUTION);
		assertThat(workflow.status()).isEqualTo(WorkflowStatus.RUNNING);
		assertThat(workflow.completedStages()).containsExactly(WorkflowStage.ANALYZING_REQUIREMENTS);
	}

	@Test
	void rejectsIllegalTransitionWithoutChangingState() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", "o", "r", 42));

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
				new WorkflowContext("https://github.com/o/r", "o", "r", 42));

		workflow.attachExecution("exec-1");

		assertThat(workflow.executionId()).isEqualTo("exec-1");
		assertThat(workflow.checkpoints()).contains(new Checkpoint("execution", "exec-1"));
	}

	@Test
	void markRunning_shouldTransitionCreatedToRunning() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", "o", "r", 42));

		workflow.markRunning();

		assertThat(workflow.status()).isEqualTo(WorkflowStatus.RUNNING);
	}

	@Test
	void recordAnalysis_shouldStoreResult() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", "o", "r", 42));

		workflow.recordAnalysis("feature/issue-42", "текст анализа");

		assertThat(workflow.branchName()).isEqualTo("feature/issue-42");
		assertThat(workflow.analysisText()).isEqualTo("текст анализа");
	}

	@Test
	void continueAfterAnalysis_withoutResult_shouldReject() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", "o", "r", 42));

		assertThatThrownBy(workflow::continueAfterAnalysis)
				.isInstanceOf(IllegalWorkflowTransitionException.class);

		assertThat(workflow.currentStage()).isEqualTo(WorkflowStage.ANALYZING_REQUIREMENTS);
	}

	@Test
	void continueAfterAnalysis_withResult_shouldAdvanceStage() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", "o", "r", 42));
		workflow.recordAnalysis("feature/issue-42", "текст анализа");

		workflow.continueAfterAnalysis();

		assertThat(workflow.currentStage()).isEqualTo(WorkflowStage.DESIGNING_SOLUTION);
		assertThat(workflow.status()).isEqualTo(WorkflowStatus.RUNNING);
	}

	@Test
	void fail_shouldStoreReason() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", "o", "r", 42));

		workflow.fail("boom");

		assertThat(workflow.status()).isEqualTo(WorkflowStatus.FAILED);
		assertThat(workflow.failureReason()).isEqualTo("boom");
	}

	@Test
	void continueAfterAnalysis_shouldClearResult() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", "o", "r", 42));
		workflow.recordAnalysis("feature/issue-42", "текст анализа");

		workflow.continueAfterAnalysis();

		assertThat(workflow.currentStage()).isEqualTo(WorkflowStage.DESIGNING_SOLUTION);
		assertThat(workflow.analysisText()).isNull();
		assertThat(workflow.branchName()).isEqualTo("feature/issue-42");
	}

	@Test
	void continueAfterAnalysis_twice_shouldReject() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", "o", "r", 42));
		workflow.recordAnalysis("feature/issue-42", "текст анализа");
		workflow.continueAfterAnalysis();

		assertThatThrownBy(workflow::continueAfterAnalysis)
				.isInstanceOf(IllegalWorkflowTransitionException.class);
	}

	@Test
	void rework_shouldResetToAnalyzingRequirements() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", "o", "r", 42));
		workflow.recordAnalysis("feature/issue-42", "текст анализа");

		workflow.rework("добавь риски");

		assertThat(workflow.currentStage()).isEqualTo(WorkflowStage.ANALYZING_REQUIREMENTS);
		assertThat(workflow.analysisText()).isNull();
		assertThat(workflow.branchName()).isNull();
		assertThat(workflow.status()).isEqualTo(WorkflowStatus.RUNNING);
	}

	@Test
	void rework_withoutResult_shouldReject() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", "o", "r", 42));

		assertThatThrownBy(() -> workflow.rework("feedback"))
				.isInstanceOf(IllegalWorkflowTransitionException.class);
	}

	@Test
	void rework_afterContinue_shouldReject() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/o/r", "o", "r", 42));
		workflow.recordAnalysis("feature/issue-42", "текст анализа");
		workflow.continueAfterAnalysis();

		assertThatThrownBy(() -> workflow.rework("feedback"))
				.isInstanceOf(IllegalWorkflowTransitionException.class);
	}
}
