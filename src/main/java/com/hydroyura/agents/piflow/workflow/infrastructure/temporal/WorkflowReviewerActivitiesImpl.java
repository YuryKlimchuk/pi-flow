package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.hydroyura.agents.piflow.agent.api.AgentOperations;
import com.hydroyura.agents.piflow.agent.api.AgentRole;
import com.hydroyura.agents.piflow.agent.api.AgentRunResult;
import com.hydroyura.agents.piflow.agent.api.AgentRunStatus;
import com.hydroyura.agents.piflow.agent.api.StartAgentCommand;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация activity независимого ревью через публичный контракт модуля
 * {@code agent} с ролью {@code REVIEWER}.
 *
 * <p>Activity возвращает текст ревью из stdout агента и не интерпретирует его как
 * бизнес-решение о приемлемости.</p>
 */
@Slf4j
public class WorkflowReviewerActivitiesImpl implements WorkflowReviewerActivities {

	private final AgentOperations agentOperations;

	public WorkflowReviewerActivitiesImpl(AgentOperations agentOperations) {
		this.agentOperations = agentOperations;
	}

	@Override
	public WorkflowReviewerResult review(WorkflowReviewerInput input) {
		Path workspace = validateInput(input);
		log.info("reviewer activity started: workflowId={} workspace={}", input.workflowId(), workspace);
		StartAgentCommand command = new StartAgentCommand(
				AgentRole.REVIEWER,
				buildInstruction(input, workspace),
				issueContext(input),
				input.repositoryUrl(),
				workspace,
				java.util.Map.of(),
				null,
				null);

		AgentRunResult result;
		try {
			result = agentOperations.run(command);
		} catch (RuntimeException e) {
			log.error("reviewer activity failed: workflowId={}", input.workflowId(), e);
			throw WorkflowActivityException.agentFailed("reviewer run failed: " + e.getMessage(), e);
		}
		validateRunResult(result);

		String reviewText = result.stdout() == null ? "" : result.stdout().strip();
		if (reviewText.isBlank()) {
			reviewText = result.stderr() == null ? "" : result.stderr().strip();
		}
		if (reviewText.isBlank()) {
			throw WorkflowActivityException.invalidResult("review result is empty");
		}
		log.info("reviewer activity finished: workflowId={} runId={}", input.workflowId(), result.runId());
		return new WorkflowReviewerResult(result.runId(), reviewText);
	}

	private Path validateInput(WorkflowReviewerInput input) {
		if (input == null || input.workspacePath() == null || input.workspacePath().isBlank()) {
			throw WorkflowActivityException.invalidResult("workspace path is missing");
		}
		Path workspace = Path.of(input.workspacePath());
		if (!Files.isDirectory(workspace)) {
			throw WorkflowActivityException.invalidResult("workspace does not exist: " + workspace);
		}
		return workspace;
	}

	private String buildInstruction(WorkflowReviewerInput input, Path workspace) {
		String instruction = """
				Выполни независимое ревью реализованного решения задачи.

				Issue: %s/%s#%d
				Репозиторий: %s
				Рабочая директория проекта: %s

				Изучи изменения в рабочей директории и оцени корректность, полноту и качество реализации.
				Верни результат ревью текстом: найденные проблемы, замечания и рекомендации.
				""".formatted(
				input.owner(), input.repository(), input.issueNumber(),
				input.repositoryUrl(), workspace);
		if (input.implementationDiagnostics() != null && !input.implementationDiagnostics().isBlank()) {
			instruction += "\n\nДиагностика сборки/тестов реализации:\n" + input.implementationDiagnostics();
		}
		return instruction;
	}

	private String issueContext(WorkflowReviewerInput input) {
		return "Issue " + input.owner() + "/" + input.repository() + "#" + input.issueNumber();
	}

	private void validateRunResult(AgentRunResult result) {
		if (result == null) {
			throw WorkflowActivityException.agentFailed("reviewer returned no result");
		}
		if (result.status() != AgentRunStatus.SUCCEEDED) {
			throw WorkflowActivityException.agentFailed("reviewer finished with status " + result.status());
		}
		if (result.exitCode() != null && result.exitCode() != 0) {
			throw WorkflowActivityException.agentFailed("reviewer finished with exit code " + result.exitCode());
		}
	}
}
