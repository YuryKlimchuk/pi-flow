package com.hydroyura.agents.piflow.workflow.application;

import java.util.ArrayList;
import java.util.List;

import com.hydroyura.agents.piflow.workspace.api.WorkspaceOperations;
import com.hydroyura.agents.piflow.workflow.api.ContinueWorkflowCommand;
import com.hydroyura.agents.piflow.workflow.api.ReworkWorkflowCommand;
import com.hydroyura.agents.piflow.workflow.api.StartWorkflowCommand;
import com.hydroyura.agents.piflow.workflow.api.WorkflowException;
import com.hydroyura.agents.piflow.workflow.api.WorkflowOperations;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.api.WorkflowState;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStatus;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStepInfo;
import com.hydroyura.agents.piflow.workflow.api.WorkflowSummary;
import com.hydroyura.agents.piflow.workflow.domain.IllegalWorkflowTransitionException;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import com.hydroyura.agents.piflow.workflow.domain.WorkflowContext;
import com.hydroyura.agents.piflow.workflow.domain.WorkflowId;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Реализует публичный порт управления единым длительным Workflow.
 */
@Slf4j
@Service
public class WorkflowService implements WorkflowOperations {

	private final WorkflowExecutionPort executionPort;
	private final WorkflowRepository repository;
	private final WorkspaceOperations workspaceOperations;

	public WorkflowService(
			WorkflowExecutionPort executionPort,
			WorkflowRepository repository,
			WorkspaceOperations workspaceOperations) {
		this.executionPort = executionPort;
		this.repository = repository;
		this.workspaceOperations = workspaceOperations;
	}

	@Override
	public WorkflowState start(StartWorkflowCommand command) {
		command.validate();
		String repositoryUrl = cloneUrl(command.owner(), command.repository());
		Workflow workflow = Workflow.create(
				new WorkflowId(command.workflowId()),
				new WorkflowContext(repositoryUrl, command.owner(), command.repository(), command.issueNumber()));
		try {
			WorkflowExecutionResult execution = executionPort.start(new WorkflowExecutionCommand(
					command.workflowId(), repositoryUrl, command.owner(), command.repository(), command.issueNumber()));
			workflow.attachExecution(execution.executionId());
			workflow.markRunning();
			repository.save(workflow);
			log.info("workflow started: workflowId={} owner={} repository={} issue={}",
					command.workflowId(), command.owner(), command.repository(), command.issueNumber());
			return toState(workflow);
		} catch (WorkflowException e) {
			workflow.fail("execution start failed");
			repository.save(workflow);
			log.error("workflow start failed: workflowId={} error={}", command.workflowId(), e.getMessage());
			throw e;
		}
	}

	@Override
	public WorkflowState getState(String workflowId) {
		if (workflowId == null || workflowId.isBlank()) {
			throw WorkflowException.validation("workflowId must not be blank");
		}
		Workflow workflow = repository.findById(new WorkflowId(workflowId))
				.orElseThrow(() -> WorkflowException.notFound("workflow not found: " + workflowId));
		syncExecution(workflow);
		return toState(workflow);
	}

	@Override
	public WorkflowState continueWorkflow(ContinueWorkflowCommand command) {
		command.validate();
		Workflow workflow = repository.findById(new WorkflowId(command.workflowId()))
				.orElseThrow(() -> WorkflowException.notFound("workflow not found: " + command.workflowId()));
		try {
			switch (workflow.currentStage()) {
				case ANALYZING_REQUIREMENTS -> workflow.continueAfterAnalysis();
				case DESIGNING_SOLUTION -> workflow.continueAfterDesign();
				case REVIEWING -> workflow.approveReview();
				default -> throw new IllegalWorkflowTransitionException(
						"cannot continue from stage " + workflow.currentStage());
			}
		} catch (IllegalWorkflowTransitionException e) {
			throw WorkflowException.illegalTransition(e.getMessage());
		}
		executionPort.continueStage(workflow.executionId());
		repository.save(workflow);
		return toState(workflow);
	}

	@Override
	public WorkflowState rework(ReworkWorkflowCommand command) {
		command.validate();
		Workflow workflow = repository.findById(new WorkflowId(command.workflowId()))
				.orElseThrow(() -> WorkflowException.notFound("workflow not found: " + command.workflowId()));
		try {
			switch (workflow.currentStage()) {
				case ANALYZING_REQUIREMENTS -> workflow.rework(command.feedback());
				case DESIGNING_SOLUTION -> workflow.reworkDesign(command.feedback());
				case IMPLEMENTING -> workflow.reworkImplementation(command.feedback());
				case REVIEWING -> workflow.reworkReview(command.feedback());
				default -> throw new IllegalWorkflowTransitionException(
						"cannot rework from stage " + workflow.currentStage());
			}
		} catch (IllegalWorkflowTransitionException e) {
			throw WorkflowException.illegalTransition(e.getMessage());
		}
		executionPort.reworkStage(workflow.executionId(), command.feedback());
		repository.save(workflow);
		return toState(workflow);
	}

	@Override
	public List<WorkflowSummary> listWorkflows() {
		return repository.findAll().stream()
				.map(w -> {
					try {
						syncExecution(w);
					} catch (RuntimeException ignored) {
						// при недоступности Temporal/workspace оставляем последнее известное состояние.
					}
					return new WorkflowSummary(
							w.id().value(),
							w.context().repositoryUrl(),
							w.context().issueNumber(),
							w.status().name(),
							w.currentStage().name());
				})
				.toList();
	}

	private void syncExecution(Workflow workflow) {
		if (workflow.executionId() == null || isTerminal(workflow)) {
			return;
		}
		WorkflowExecutionState state = executionPort.queryState(workflow.executionId());
		switch (state.status()) {
			case "FAILED" -> {
				workflow.fail(state.failureReason());
				repository.save(workflow);
				log.info("workflow failed: workflowId={} reason={}", workflow.id().value(), state.failureReason());
			}
			case "CANCELLED" -> {
				workflow.cancel();
				repository.save(workflow);
				log.info("workflow cancelled: workflowId={}", workflow.id().value());
			}
			case "COMPLETED" -> {
				if (workflow.currentStage() == com.hydroyura.agents.piflow.workflow.domain.WorkflowStage.READY_FOR_PULL_REQUEST) {
					workflow.finalize(state.commitId(), state.branchUrl(), state.prNumber(), state.prUrl());
					repository.save(workflow);
					log.info("workflow completed: workflowId={} pr={} url={}",
							workflow.id().value(), state.prNumber(), state.prUrl());
				}
			}
			case "WAITING_FOR_USER" -> applyWaiting(workflow, state);
			case "RUNNING" -> applyRunning(workflow, state);
			default -> {
				// неизвестный статус не меняет доменное состояние.
			}
		}
	}

	private void applyWaiting(Workflow workflow, WorkflowExecutionState state) {
		switch (state.stage()) {
			case "ANALYZING_REQUIREMENTS" -> {
				if (workflow.analysisText() == null) {
					workflow.recordAnalysis(state.branchName(), analysisArtifact(state));
					repository.save(workflow);
					log.info("analysis recorded: workflowId={} branch={}", workflow.id().value(), state.branchName());
				}
			}
			case "DESIGNING_SOLUTION" -> {
				if (workflow.designText() == null) {
					workflow.recordDesign(designArtifact(state));
					repository.save(workflow);
					log.info("design recorded: workflowId={}", workflow.id().value());
				}
			}
			case "IMPLEMENTING" -> {
				if (workflow.status() == com.hydroyura.agents.piflow.workflow.domain.WorkflowStatus.RUNNING) {
					workflow.recordImplementation(state.implementationRunId(), state.implementationDiagnostics(), false);
					repository.save(workflow);
					log.info("implementation failed, waiting for rework: workflowId={} diagnostics={}",
							workflow.id().value(), state.implementationDiagnostics());
				}
			}
			case "REVIEWING" -> {
				if (workflow.reviewText() == null) {
					workflow.recordReview(state.reviewRunId(), state.reviewText());
					repository.save(workflow);
					log.info("review recorded: workflowId={}", workflow.id().value());
				}
			}
			default -> {
				// прочие этапы не имеют ручного gate.
			}
		}
	}

	private void applyRunning(Workflow workflow, WorkflowExecutionState state) {
		if ("REVIEWING".equals(state.stage())
				&& workflow.currentStage() == com.hydroyura.agents.piflow.workflow.domain.WorkflowStage.IMPLEMENTING) {
			workflow.recordImplementation(state.implementationRunId(), state.implementationDiagnostics(), true);
			repository.save(workflow);
		}
	}

	private boolean isTerminal(Workflow workflow) {
		return workflow.status() == com.hydroyura.agents.piflow.workflow.domain.WorkflowStatus.FAILED
				|| workflow.status() == com.hydroyura.agents.piflow.workflow.domain.WorkflowStatus.COMPLETED
				|| workflow.status() == com.hydroyura.agents.piflow.workflow.domain.WorkflowStatus.CANCELLED;
	}

	private String analysisArtifact(WorkflowExecutionState state) {
		try {
			return workspaceOperations.readAnalysisArtifact(state.workspacePath()).text();
		} catch (RuntimeException e) {
			throw WorkflowException.orchestration("analysis artifact unavailable: " + e.getMessage(), e);
		}
	}

	private String designArtifact(WorkflowExecutionState state) {
		try {
			return workspaceOperations.readDesignArtifact(state.workspacePath()).text();
		} catch (RuntimeException e) {
			throw WorkflowException.orchestration("design artifact unavailable: " + e.getMessage(), e);
		}
	}

	private String cloneUrl(String owner, String repository) {
		return "https://github.com/" + owner + "/" + repository + ".git";
	}

	private WorkflowState toState(Workflow workflow) {
		List<WorkflowStage> completed = new ArrayList<>();
		for (com.hydroyura.agents.piflow.workflow.domain.WorkflowStage stage : workflow.completedStages()) {
			completed.add(WorkflowStage.valueOf(stage.name()));
		}
		List<WorkflowStepInfo> timeline = new ArrayList<>();
		for (com.hydroyura.agents.piflow.workflow.domain.CompletedWorkflowStep step : workflow.timeline()) {
			timeline.add(new WorkflowStepInfo(
					step.stage(), step.role(), step.action(), step.result(), step.runId()));
		}
		return new WorkflowState(
				workflow.id().value(),
				workflow.context().repositoryUrl(),
				workflow.context().issueNumber(),
				WorkflowStatus.valueOf(workflow.status().name()),
				WorkflowStage.valueOf(workflow.currentStage().name()),
				List.copyOf(completed),
				workflow.executionId(),
				workflow.analysisText(),
				workflow.branchName(),
				workflow.designText(),
				workflow.implementationRunId(),
				workflow.implementationDiagnostics(),
				workflow.reviewText(),
				workflow.reviewRunId(),
				workflow.failureReason(),
				List.copyOf(timeline),
				workflow.commitId(),
				workflow.branchUrl(),
				workflow.prNumber(),
				workflow.prUrl());
	}
}
