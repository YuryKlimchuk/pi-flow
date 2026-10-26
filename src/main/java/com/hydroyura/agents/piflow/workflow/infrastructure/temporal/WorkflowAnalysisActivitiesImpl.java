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

/**
 * Реализация activity анализа проекта через публичный контракт модуля {@code agent}.
 *
 * <p>Activity вызывает только {@link AgentOperations} с ролью {@code ANALYST},
 * передаёт контекст исходного Issue и путь подготовленного workspace, а после
 * завершения валидирует статус запуска, сохранённый артефакт и предложенное имя
 * ветки. Не выполняет запуск процессов напрямую и не содержит Temporal-логики
 * вне объявления activity.</p>
 */
public class WorkflowAnalysisActivitiesImpl implements WorkflowAnalysisActivities {

	static final String ARTIFACT_RELATIVE_PATH = ".pi-flow/analysis/analyst.md";
	static final String BRANCH_RELATIVE_PATH = ".pi-flow/analysis/branch.txt";

	private final AgentOperations agentOperations;

	public WorkflowAnalysisActivitiesImpl(AgentOperations agentOperations) {
		this.agentOperations = agentOperations;
	}

	@Override
	public WorkflowAnalysisResult analyze(WorkflowAnalysisInput input) {
		Path workspace = validateInput(input);
		StartAgentCommand command = new StartAgentCommand(
				AgentRole.ANALYST,
				buildInstruction(input, workspace),
				issueContext(input),
				input.repositoryUrl(),
				workspace,
				java.util.Map.of(),
				null);

		AgentRunResult result;
		try {
			result = agentOperations.run(command);
		} catch (RuntimeException e) {
			throw WorkflowAnalysisException.agentFailed("agent run failed: " + e.getMessage());
		}

		validateRunResult(result);

		Path artifact = workspace.resolve(ARTIFACT_RELATIVE_PATH);
		validateArtifact(artifact);

		long size;
		try {
			size = Files.size(artifact);
		} catch (IOException e) {
			throw WorkflowAnalysisException.invalidResult("analysis artifact cannot be read: " + artifact);
		}

		String branchName = readBranchName(workspace);
		return new WorkflowAnalysisResult(workspace.toString(), branchName, result.runId(), size);
	}

	private Path validateInput(WorkflowAnalysisInput input) {
		if (input == null || input.workspacePath() == null || input.workspacePath().isBlank()) {
			throw WorkflowAnalysisException.invalidResult("workspace path is missing");
		}
		Path workspace = Path.of(input.workspacePath());
		if (!Files.isDirectory(workspace)) {
			throw WorkflowAnalysisException.invalidResult("workspace does not exist: " + workspace);
		}
		return workspace;
	}

	private String buildInstruction(WorkflowAnalysisInput input, Path workspace) {
		return """
				Проанализируй проект и исходную задачу GitHub Issue.

				Issue: %s/%s#%d
				Репозиторий: %s
				Рабочая директория проекта: %s

				Требуется первичный анализ задачи и проекта (НЕ OpenSpec Proposal). Изучи Issue и структуру проекта в рабочей директории.

				Запиши результат анализа в файл:
				%s

				Используй разделы: Понимание задачи; Контекст проекта; Ограничения и предположения; Релевантные модули и файлы; Риски и аспекты реализации; Открытые вопросы.

				Также предложи имя рабочей ветки для реализации (только латиница и допустимые символы Git: буквы, цифры, '-', '_', '/', '.') и запиши его отдельной строкой в файл:
				%s
				""".formatted(
				input.owner(), input.repository(), input.issueNumber(),
				input.repositoryUrl(), workspace,
				workspace.resolve(ARTIFACT_RELATIVE_PATH),
				workspace.resolve(BRANCH_RELATIVE_PATH));
	}

	private String issueContext(WorkflowAnalysisInput input) {
		return "Issue " + input.owner() + "/" + input.repository() + "#" + input.issueNumber();
	}

	private void validateRunResult(AgentRunResult result) {
		if (result == null) {
			throw WorkflowAnalysisException.agentFailed("agent returned no result");
		}
		if (result.status() != AgentRunStatus.SUCCEEDED) {
			throw WorkflowAnalysisException.agentFailed("agent finished with status " + result.status());
		}
		if (result.exitCode() != null && result.exitCode() != 0) {
			throw WorkflowAnalysisException.agentFailed("agent finished with exit code " + result.exitCode());
		}
	}

	private void validateArtifact(Path artifact) {
		if (!Files.isRegularFile(artifact)) {
			throw WorkflowAnalysisException.invalidResult("analysis artifact is missing: " + artifact);
		}
		if (!Files.isReadable(artifact)) {
			throw WorkflowAnalysisException.invalidResult("analysis artifact is not readable: " + artifact);
		}
		try {
			String content = Files.readString(artifact, StandardCharsets.UTF_8);
			if (content == null || content.isBlank()) {
				throw WorkflowAnalysisException.invalidResult("analysis artifact is empty: " + artifact);
			}
		} catch (IOException e) {
			throw WorkflowAnalysisException.invalidResult("analysis artifact cannot be read: " + artifact);
		}
	}

	private String readBranchName(Path workspace) {
		Path branchFile = workspace.resolve(BRANCH_RELATIVE_PATH).normalize();
		if (!Files.isRegularFile(branchFile) || !Files.isReadable(branchFile)) {
			throw WorkflowAnalysisException.invalidResult("branch suggestion file is missing: " + branchFile);
		}
		try {
			String branchName = Files.readString(branchFile, StandardCharsets.UTF_8).trim();
			if (!isValidBranchName(branchName)) {
				throw WorkflowAnalysisException.invalidResult("invalid branch name suggested: " + branchName);
			}
			return branchName;
		} catch (IOException e) {
			throw WorkflowAnalysisException.invalidResult("branch suggestion cannot be read: " + branchFile);
		}
	}

	static boolean isValidBranchName(String name) {
		if (name == null || name.isBlank()) {
			return false;
		}
		if (name.startsWith("/") || name.endsWith("/") || name.endsWith(".")) {
			return false;
		}
		if (name.contains("..") || name.contains("@{") || name.contains("//")) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			boolean letter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
			boolean digit = c >= '0' && c <= '9';
			boolean allowed = c == '-' || c == '_' || c == '/' || c == '.';
			if (!letter && !digit && !allowed) {
				return false;
			}
		}
		return true;
	}
}
