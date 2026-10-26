package com.hydroyura.agents.piflow.agent.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

class StartAgentCommandTest {

	@Test
	void validate_whenAllRequiredFieldsPresent_shouldPass() {
		var command = new StartAgentCommand(AgentRole.ANALYST, "do", null, null, Path.of("."), Map.of(), null, null);

		Throwable error = catchThrowable(command::validate);

		assertThat(error).isNull();
	}

	@Test
	void validate_whenRoleNull_shouldThrowPreparation() {
		var command = new StartAgentCommand(null, "do", null, null, Path.of("."), Map.of(), null, null);

		AgentException error = catchThrowableOfType(command::validate, AgentException.class);

		assertThat(error.errorType()).isEqualTo(AgentException.ErrorType.PREPARATION);
	}

	@Test
	void validate_whenInstructionBlank_shouldThrowPreparation() {
		var command = new StartAgentCommand(AgentRole.ANALYST, "  ", null, null, Path.of("."), Map.of(), null, null);

		AgentException error = catchThrowableOfType(command::validate, AgentException.class);

		assertThat(error.errorType()).isEqualTo(AgentException.ErrorType.PREPARATION);
	}

	@Test
	void validate_whenWorkingDirectoryNull_shouldThrowPreparation() {
		var command = new StartAgentCommand(AgentRole.ANALYST, "do", null, null, null, Map.of(), null, null);

		AgentException error = catchThrowableOfType(command::validate, AgentException.class);

		assertThat(error.errorType()).isEqualTo(AgentException.ErrorType.PREPARATION);
	}

	@Test
	void validate_whenTimeoutNegative_shouldThrowPreparation() {
		var command = new StartAgentCommand(AgentRole.ANALYST, "do", null, null, Path.of("."), Map.of(),
				Duration.ofSeconds(-1), null);

		AgentException error = catchThrowableOfType(command::validate, AgentException.class);

		assertThat(error.errorType()).isEqualTo(AgentException.ErrorType.PREPARATION);
	}
}
