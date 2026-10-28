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
 * Реализация activity проектирования через роль {@code ARCHITECT}.
 */
@Slf4j
public class WorkflowDesignActivitiesImpl implements WorkflowDesignActivities {

	static final String DESIGN_RELATIVE_PATH = ".pi-flow/design/architect.md";

	private final AgentOperations agentOperations;

	public WorkflowDesignActivitiesImpl(AgentOperations agentOperations) {
		this.agentOperations = agentOperations;
	}

	@Override
	public WorkflowAnalysisResult design(WorkflowDesignInput input) {
		Path workspace = validateInput(input);
		StartAgentCommand command = new StartAgentCommand(
				AgentRole.ARCHITECT,
				buildInstruction(input, workspace),
				issueContext(input),
				input.repositoryUrl(),
				workspace,
				java.util.Map.of(),
				null,
				input.analysisText());

		AgentRunResult result;
		try {
			result = agentOperations.run(command);
		} catch (RuntimeException e) {
			log.error("design activity failed: workflowId={}", input.workflowId(), e);
			throw WorkflowAnalysisException.agentFailed("architect run failed: " + e.getMessage(), e);
		}

		validateRunResult(result);

		Path artifact = workspace.resolve(DESIGN_RELATIVE_PATH);
		validateArtifact(artifact);

		long size;
		try {
			size = Files.size(artifact);
		} catch (IOException e) {
			throw WorkflowAnalysisException.invalidResult("design artifact cannot be read: " + artifact);
		}
		return new WorkflowAnalysisResult(workspace.toString(), null, result.runId(), size);
	}

	private Path validateInput(WorkflowDesignInput input) {
		if (input == null || input.workspacePath() == null || input.workspacePath().isBlank()) {
			throw WorkflowAnalysisException.invalidResult("workspace path is missing");
		}
		Path workspace = Path.of(input.workspacePath());
		if (!Files.isDirectory(workspace)) {
			throw WorkflowAnalysisException.invalidResult("workspace does not exist: " + workspace);
		}
		return workspace;
	}

	private String buildInstruction(WorkflowDesignInput input, Path workspace) {
		String instruction = """
				Спроектируй решение задачи на основе выполненного анализа.

				Issue: %s/%s#%d
				Репозиторий: %s
				Рабочая директория проекта: %s

				Запиши проектное решение в файл:
				%s

				Используй разделы: Решение; Декомпозиция; Затрагиваемые модули и файлы; Компромиссы; Риски и допущения.
				""".formatted(
				input.owner(), input.repository(), input.issueNumber(),
				input.repositoryUrl(), workspace,
				workspace.resolve(DESIGN_RELATIVE_PATH));
		String analysisText = input.analysisText();
		if (analysisText == null || analysisText.isBlank()) {
			Path analystFile = workspace.resolve(".pi-flow/analysis/analyst.md");
			if (Files.isRegularFile(analystFile)) {
				try {
					analysisText = Files.readString(analystFile, StandardCharsets.UTF_8);
				} catch (IOException ignored) {
					analysisText = null;
				}
			}
		}
		if (analysisText != null && !analysisText.isBlank()) {
			instruction += "\n\nРезультат анализа Analyst:\n" + analysisText;
		}
		return instruction;
	}

	private String issueContext(WorkflowDesignInput input) {
		return "Issue " + input.owner() + "/" + input.repository() + "#" + input.issueNumber();
	}

	private void validateRunResult(AgentRunResult result) {
		if (result == null) {
			throw WorkflowAnalysisException.agentFailed("architect returned no result");
		}
		if (result.status() != AgentRunStatus.SUCCEEDED) {
			throw WorkflowAnalysisException.agentFailed("architect finished with status " + result.status());
		}
		if (result.exitCode() != null && result.exitCode() != 0) {
			throw WorkflowAnalysisException.agentFailed("architect finished with exit code " + result.exitCode());
		}
	}

	private void validateArtifact(Path artifact) {
		if (!Files.isRegularFile(artifact)) {
			throw WorkflowAnalysisException.invalidResult("design artifact is missing: " + artifact);
		}
		if (!Files.isReadable(artifact)) {
			throw WorkflowAnalysisException.invalidResult("design artifact is not readable: " + artifact);
		}
		try {
			String content = Files.readString(artifact, StandardCharsets.UTF_8);
			if (content == null || content.isBlank()) {
				throw WorkflowAnalysisException.invalidResult("design artifact is empty: " + artifact);
			}
		} catch (IOException e) {
			throw WorkflowAnalysisException.invalidResult("design artifact cannot be read: " + artifact);
		}
	}
}
