package com.hydroyura.agents.piflow.workflow.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Проверяет переходы этапов реализации, ревью и финализации, а также лимит
 * повторных итераций и терминальное состояние.
 */
class WorkflowFlowTest {

	@Test
	void continueAfterDesign_shouldAdvanceToImplementing() {
		Workflow workflow = workflowAtDesign();

		workflow.continueAfterDesign();

		assertThat(workflow.currentStage()).isEqualTo(WorkflowStage.IMPLEMENTING);
		assertThat(workflow.status()).isEqualTo(WorkflowStatus.RUNNING);
		assertThat(workflow.designText()).isNull();
	}

	@Test
	void recordImplementation_success_shouldAdvanceToReviewing() {
		Workflow workflow = workflowAtImplementation();

		workflow.recordImplementation("run-1", "build ok", true);

		assertThat(workflow.currentStage()).isEqualTo(WorkflowStage.REVIEWING);
		assertThat(workflow.status()).isEqualTo(WorkflowStatus.RUNNING);
		assertThat(workflow.completedStages()).contains(WorkflowStage.IMPLEMENTING);
	}

	@Test
	void recordImplementation_failure_shouldWaitForUser() {
		Workflow workflow = workflowAtImplementation();

		workflow.recordImplementation("run-1", "build failed", false);

		assertThat(workflow.currentStage()).isEqualTo(WorkflowStage.IMPLEMENTING);
		assertThat(workflow.status()).isEqualTo(WorkflowStatus.WAITING_FOR_USER);
		assertThat(workflow.implementationDiagnostics()).isEqualTo("build failed");
	}

	@Test
	void reworkImplementation_shouldResetAndCount() {
		Workflow workflow = workflowAtImplementation();
		workflow.recordImplementation("run-1", "build failed", false);

		workflow.reworkImplementation("исправь сборку");

		assertThat(workflow.status()).isEqualTo(WorkflowStatus.RUNNING);
		assertThat(workflow.currentStage()).isEqualTo(WorkflowStage.IMPLEMENTING);
		assertThat(workflow.implementationReworkCount()).isEqualTo(1);
		assertThat(workflow.implementationDiagnostics()).isNull();
	}

	@Test
	void reworkImplementation_fourth_shouldFailWorkflow() {
		Workflow workflow = workflowAtImplementation();
		for (int i = 0; i < 3; i++) {
			workflow.recordImplementation("run-" + i, "failed", false);
			workflow.reworkImplementation("feedback " + i);
		}
		workflow.recordImplementation("run-3", "failed", false);

		workflow.reworkImplementation("четвёртый rework");

		assertThat(workflow.status()).isEqualTo(WorkflowStatus.FAILED);
		assertThat(workflow.failureReason()).isEqualTo("implementation rework limit exceeded");
	}

	@Test
	void recordReview_shouldWaitForUser() {
		Workflow workflow = workflowAtReviewing();

		workflow.recordReview("run-2", "есть замечания");

		assertThat(workflow.currentStage()).isEqualTo(WorkflowStage.REVIEWING);
		assertThat(workflow.status()).isEqualTo(WorkflowStatus.WAITING_FOR_USER);
		assertThat(workflow.reviewText()).isEqualTo("есть замечания");
	}

	@Test
	void approveReview_shouldAdvanceToReadyForPullRequest() {
		Workflow workflow = workflowAtReviewing();
		workflow.recordReview("run-2", "ок");

		workflow.approveReview();

		assertThat(workflow.currentStage()).isEqualTo(WorkflowStage.READY_FOR_PULL_REQUEST);
		assertThat(workflow.status()).isEqualTo(WorkflowStatus.RUNNING);
	}

	@Test
	void reworkReview_shouldReturnToImplementing() {
		Workflow workflow = workflowAtReviewing();
		workflow.recordReview("run-2", "замечания");

		workflow.reworkReview("доработай");

		assertThat(workflow.currentStage()).isEqualTo(WorkflowStage.IMPLEMENTING);
		assertThat(workflow.status()).isEqualTo(WorkflowStatus.RUNNING);
		assertThat(workflow.reviewReworkCount()).isEqualTo(1);
		assertThat(workflow.reviewText()).isNull();
	}

	@Test
	void reworkReview_fourth_shouldFailWorkflow() {
		Workflow workflow = workflowAtReviewing();
		for (int i = 0; i < 3; i++) {
			workflow.recordReview("run-" + i, "замечания");
			workflow.reworkReview("feedback " + i);
			workflow.recordImplementation("run-impl-" + i, "ok", true);
		}
		workflow.recordReview("run-3", "замечания");

		workflow.reworkReview("четвёртый rework");

		assertThat(workflow.status()).isEqualTo(WorkflowStatus.FAILED);
		assertThat(workflow.failureReason()).isEqualTo("review rework limit exceeded");
	}

	@Test
	void finalize_shouldCompleteWithPr() {
		Workflow workflow = workflowAtReadyForPullRequest();

		workflow.finalize("commit-1", "https://github.com/acme/demo/tree/feature", 7, "https://github.com/acme/demo/pull/7");

		assertThat(workflow.status()).isEqualTo(WorkflowStatus.COMPLETED);
		assertThat(workflow.commitId()).isEqualTo("commit-1");
		assertThat(workflow.prNumber()).isEqualTo(7);
		assertThat(workflow.prUrl()).isEqualTo("https://github.com/acme/demo/pull/7");
	}

	@Test
	void approveReview_withoutResult_shouldRejectWithoutChange() {
		Workflow workflow = workflowAtReviewing();

		assertThatThrownBy(workflow::approveReview)
				.isInstanceOf(IllegalWorkflowTransitionException.class);

		assertThat(workflow.currentStage()).isEqualTo(WorkflowStage.REVIEWING);
		assertThat(workflow.status()).isEqualTo(WorkflowStatus.RUNNING);
	}

	private Workflow workflowAtDesign() {
		Workflow workflow = Workflow.create(
				new WorkflowId("wf-1"),
				new WorkflowContext("https://github.com/acme/demo.git", "acme", "demo", 42));
		workflow.markRunning();
		workflow.recordAnalysis("feature/issue-42", "анализ");
		workflow.continueAfterAnalysis();
		workflow.recordDesign("проект");
		return workflow;
	}

	private Workflow workflowAtImplementation() {
		Workflow workflow = workflowAtDesign();
		workflow.continueAfterDesign();
		return workflow;
	}

	private Workflow workflowAtReviewing() {
		Workflow workflow = workflowAtImplementation();
		workflow.recordImplementation("run-1", "build ok", true);
		return workflow;
	}

	private Workflow workflowAtReadyForPullRequest() {
		Workflow workflow = workflowAtReviewing();
		workflow.recordReview("run-2", "ок");
		workflow.approveReview();
		return workflow;
	}
}
