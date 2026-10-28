package com.hydroyura.agents.piflow.agent.application;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.hydroyura.agents.piflow.agent.api.AgentException;
import com.hydroyura.agents.piflow.agent.api.AgentOperations;
import com.hydroyura.agents.piflow.agent.api.AgentRunResult;
import com.hydroyura.agents.piflow.agent.api.AgentRunStatus;
import com.hydroyura.agents.piflow.agent.api.StartAgentCommand;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessException;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessRequest;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessResult;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessRunner;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Реализует публичный порт запуска задачи агента через внутренний порт запуска процесса.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService implements AgentOperations {

	private final AgentProcessRunner runner;

	@Override
	public AgentRunResult run(StartAgentCommand command) {
		command.validate();
		validateWorkingDirectory(command.workingDirectory());

		String runId = UUID.randomUUID().toString();
		log.info("agent run started: runId={} role={} workingDirectory={}",
				runId, command.role(), command.workingDirectory());
		log.debug("agent request: runId={} instruction={} inputData={} context={} feedback={}",
				runId, command.instruction(), command.inputData(), command.context(), command.feedback());
		AgentProcessRequest request = toRequest(command, runId);

		try {
			AgentProcessResult result = runner.execute(request);
			log.info("agent run finished: runId={} status={} exitCode={}",
					runId, result.status(), result.exitCode());
			log.debug("agent response: runId={} stdout={} stderr={}",
					runId, result.stdout(), result.stderr());
			return toResult(command, result);
		} catch (AgentProcessException e) {
			log.warn("agent run failed: runId={} error={}", runId, e.getMessage(), e);
			throw AgentErrors.map(e);
		}
	}

	@Override
	public void cancel(String runId) {
		runner.cancel(runId);
	}

	private AgentProcessRequest toRequest(StartAgentCommand command, String runId) {
		return new AgentProcessRequest(
				runId,
				command.role().name(),
				command.instruction(),
				command.inputData(),
				command.context(),
				command.workingDirectory(),
				command.parameters(),
				command.timeout(),
				command.feedback());
	}

	private AgentRunResult toResult(StartAgentCommand command, AgentProcessResult result) {
		Map<String, String> metadata = new LinkedHashMap<>();
		metadata.put("role", command.role().name());
		metadata.put("workingDirectory", command.workingDirectory().toString());

		return new AgentRunResult(
				result.runId(),
				command.role(),
				toStatus(result.status()),
				result.stdout(),
				result.stderr(),
				result.exitCode(),
				Map.copyOf(metadata));
	}

	private AgentRunStatus toStatus(AgentProcessStatus status) {
		return switch (status) {
			case SUCCEEDED -> AgentRunStatus.SUCCEEDED;
			case FAILED -> AgentRunStatus.FAILED;
			case TIMED_OUT -> AgentRunStatus.TIMED_OUT;
			case CANCELLED -> AgentRunStatus.CANCELLED;
		};
	}

	private void validateWorkingDirectory(java.nio.file.Path workingDirectory) {
		if (!Files.isDirectory(workingDirectory) || !Files.isReadable(workingDirectory)) {
			throw AgentException.preparation("workingDirectory does not exist or is not accessible: " + workingDirectory);
		}
	}
}
