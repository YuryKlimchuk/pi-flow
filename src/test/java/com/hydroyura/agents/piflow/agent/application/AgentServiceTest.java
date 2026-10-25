package com.hydroyura.agents.piflow.agent.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import com.hydroyura.agents.piflow.agent.api.AgentException;
import com.hydroyura.agents.piflow.agent.api.AgentRole;
import com.hydroyura.agents.piflow.agent.api.AgentRunResult;
import com.hydroyura.agents.piflow.agent.api.AgentRunStatus;
import com.hydroyura.agents.piflow.agent.api.StartAgentCommand;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessException;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessResult;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessRunner;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

	@Mock
	private AgentProcessRunner runner;

	@InjectMocks
	private AgentService service;

	@TempDir
	Path tempDir;

	@Test
	void run_whenValid_shouldDelegateAndMap() {
		var command = new StartAgentCommand(AgentRole.ANALYST, "do", "input", "ctx", tempDir, Map.of(), null);
		when(runner.execute(any())).thenReturn(new AgentProcessResult("r1", AgentProcessStatus.SUCCEEDED, "out", "err", 0));

		AgentRunResult result = service.run(command);

		assertThat(result.runId()).isEqualTo("r1");
		assertThat(result.status()).isEqualTo(AgentRunStatus.SUCCEEDED);
		assertThat(result.role()).isEqualTo(AgentRole.ANALYST);
		assertThat(result.exitCode()).isZero();
		assertThat(result.metadata()).containsEntry("role", "ANALYST");
		verify(runner).execute(any());
	}

	@Test
	void run_whenWorkingDirectoryMissing_shouldThrowPreparationWithoutCallingRunner() {
		Path missing = tempDir.resolve("missing");
		var command = new StartAgentCommand(AgentRole.ANALYST, "do", "input", "ctx", missing, Map.of(), null);

		AgentException error = catchThrowableOfType(() -> service.run(command), AgentException.class);

		assertThat(error.errorType()).isEqualTo(AgentException.ErrorType.PREPARATION);
		verifyNoInteractions(runner);
	}

	@Test
	void run_whenLaunchError_shouldMapToLaunch() {
		var command = new StartAgentCommand(AgentRole.ANALYST, "do", "input", "ctx", tempDir, Map.of(), null);
		when(runner.execute(any())).thenThrow(AgentProcessException.launch("boom", new IOException("no such file")));

		AgentException error = catchThrowableOfType(() -> service.run(command), AgentException.class);

		assertThat(error.errorType()).isEqualTo(AgentException.ErrorType.LAUNCH);
	}

	@Test
	void run_whenExecutionError_shouldMapToExecution() {
		var command = new StartAgentCommand(AgentRole.ANALYST, "do", "input", "ctx", tempDir, Map.of(), null);
		when(runner.execute(any())).thenThrow(AgentProcessException.execution("interrupted", new InterruptedException()));

		AgentException error = catchThrowableOfType(() -> service.run(command), AgentException.class);

		assertThat(error.errorType()).isEqualTo(AgentException.ErrorType.EXECUTION);
	}
}
