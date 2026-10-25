package com.hydroyura.agents.piflow.workspace.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.Map;

import org.junit.jupiter.api.Test;

class WorkspacePreparationRequestTest {

	@Test
	void validate_whenValid_shouldPass() {
		var request = new WorkspacePreparationRequest("WF-1", "https://github.com/acme/repo.git", "main", Map.of());

		Throwable error = catchThrowable(request::validate);

		assertThat(error).isNull();
	}

	@Test
	void validate_whenWorkflowIdBlank_shouldThrowValidation() {
		var request = new WorkspacePreparationRequest(" ", "https://github.com/acme/repo.git", "main", Map.of());

		WorkspaceException error = catchThrowableOfType(request::validate, WorkspaceException.class);

		assertThat(error.errorType()).isEqualTo(WorkspaceException.ErrorType.VALIDATION);
	}

	@Test
	void validate_whenBranchInvalid_shouldThrowValidation() {
		var request = new WorkspacePreparationRequest("WF-1", "https://github.com/acme/repo.git", "bad branch~", Map.of());

		WorkspaceException error = catchThrowableOfType(request::validate, WorkspaceException.class);

		assertThat(error.errorType()).isEqualTo(WorkspaceException.ErrorType.VALIDATION);
	}

	@Test
	void validate_whenBranchWithDots_shouldThrowValidation() {
		var request = new WorkspacePreparationRequest("WF-1", "https://github.com/acme/repo.git", "a..b", Map.of());

		WorkspaceException error = catchThrowableOfType(request::validate, WorkspaceException.class);

		assertThat(error.errorType()).isEqualTo(WorkspaceException.ErrorType.VALIDATION);
	}
}
