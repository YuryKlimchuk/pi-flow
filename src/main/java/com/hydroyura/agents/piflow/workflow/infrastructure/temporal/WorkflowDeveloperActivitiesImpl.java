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
 * Реализация activity реализации решения через публичный контракт модуля
 * {@code agent} с ролью {@code DEVELOPER}.
 *
 * <p>Activity вызывает {@link AgentOperations#run}, передаёт утверждённый design
 * и Issue-контекст, а результат классифицирует по коду завершения: ненулевой код
 * либо статус {@code FAILED}/{@code TIMED_OUT} считается повторяемой ошибкой
 * сборки/тестов и возвращается как {@code success=false} с диагностикой.</p>
 */
@Slf4j
public class WorkflowDeveloperActivitiesImpl implements WorkflowDeveloperActivities {

	private final AgentOperations agentOperations;

	public WorkflowDeveloperActivitiesImpl(AgentOperations agentOperations) {
		this.agentOperations = agentOperations;
	}

	@Override
	public WorkflowDeveloperResult implement(WorkflowDeveloperInput input) {
		Path workspace = validateInput(input);
		log.info("developer activity started: workflowId={} workspace={}", input.workflowId(), workspace);
		StartAgentCommand command = new StartAgentCommand(
				AgentRole.DEVELOPER,
				buildInstruction(input, workspace),
				issueContext(input),
				input.repositoryUrl(),
				workspace,
				java.util.Map.of(),
				null,
				input.feedback());

		AgentRunResult result;
		try {
			result = agentOperations.run(command);
		} catch (RuntimeException e) {
			log.error("developer activity failed: workflowId={}", input.workflowId(), e);
			throw WorkflowActivityException.agentFailed("developer run failed: " + e.getMessage(), e);
		}
		if (result == null) {
			throw WorkflowActivityException.agentFailed("developer returned no result");
		}

		boolean success = result.status() == AgentRunStatus.SUCCEEDED
				&& (result.exitCode() == null || result.exitCode() == 0);
		String diagnostics = buildDiagnostics(result);
		log.info("developer activity finished: workflowId={} runId={} success={} exitCode={}",
				input.workflowId(), result.runId(), success, result.exitCode());
		return new WorkflowDeveloperResult(workspace.toString(), result.runId(), success, diagnostics);
	}

	private Path validateInput(WorkflowDeveloperInput input) {
		if (input == null || input.workspacePath() == null || input.workspacePath().isBlank()) {
			throw WorkflowActivityException.invalidResult("workspace path is missing");
		}
		Path workspace = Path.of(input.workspacePath());
		if (!Files.isDirectory(workspace)) {
			throw WorkflowActivityException.invalidResult("workspace does not exist: " + workspace);
		}
		return workspace;
	}

	private String buildInstruction(WorkflowDeveloperInput input, Path workspace) {
		String instruction = """
				Реализуй решение задачи на основе утверждённого проектного решения.

				Issue: %s/%s#%d
				Репозиторий: %s
				Рабочая директория проекта: %s

				Внеси изменения в код рабочей директории, затем запусти сборку и тесты проекта.
				Верни ненулевой код завершения, если сборка или тесты не прошли.
				""".formatted(
				input.owner(), input.repository(), input.issueNumber(),
				input.repositoryUrl(), workspace);
		String designText = input.designText();
		if (designText == null || designText.isBlank()) {
			Path designFile = workspace.resolve(".pi-flow/design/architect.md");
			if (Files.isRegularFile(designFile)) {
				try {
					designText = Files.readString(designFile, StandardCharsets.UTF_8);
				} catch (IOException ignored) {
					designText = null;
				}
			}
		}
		if (designText != null && !designText.isBlank()) {
			instruction += "\n\nУтверждённое проектное решение Architect:\n" + designText;
		}
		if (input.feedback() != null && !input.feedback().isBlank()) {
			instruction += "\n\nЗамечания пользователя (учти их при реализации):\n" + input.feedback();
		}
		return instruction;
	}

	private String issueContext(WorkflowDeveloperInput input) {
		return "Issue " + input.owner() + "/" + input.repository() + "#" + input.issueNumber();
	}

	private String buildDiagnostics(AgentRunResult result) {
		StringBuilder diagnostics = new StringBuilder();
		if (result.stdout() != null && !result.stdout().isBlank()) {
			diagnostics.append(result.stdout());
		}
		if (result.stderr() != null && !result.stderr().isBlank()) {
			if (!diagnostics.isEmpty()) {
				diagnostics.append("\n");
			}
			diagnostics.append(result.stderr());
		}
		if (result.exitCode() != null) {
			if (!diagnostics.isEmpty()) {
				diagnostics.append("\n");
			}
			diagnostics.append("exit code: ").append(result.exitCode());
		}
		return diagnostics.isEmpty() ? "no diagnostics" : diagnostics.toString();
	}
}
