package com.hydroyura.agents.piflow.github.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.Test;

class CreatePullRequestCommandTest {

	@Test
	void validate_whenAllRequiredFieldsPresent_shouldPass() {
		var command = new CreatePullRequestCommand("acme", "repo", "main", "feature/x", "Title", null);

		Throwable error = catchThrowable(command::validate);

		assertThat(error).isNull();
	}

	@Test
	void validate_whenTitleBlank_shouldThrowValidationError() {
		var command = new CreatePullRequestCommand("acme", "repo", "main", "feature/x", "  ", null);

		GithubException error = catchThrowableOfType(command::validate, GithubException.class);

		assertThat(error.errorType()).isEqualTo(GithubException.ErrorType.VALIDATION);
		assertThat(error.operation()).isEqualTo("createPullRequest");
	}

	@Test
	void validate_whenHeadBlank_shouldThrowValidationError() {
		var command = new CreatePullRequestCommand("acme", "repo", "main", "", "Title", null);

		GithubException error = catchThrowableOfType(command::validate, GithubException.class);

		assertThat(error.errorType()).isEqualTo(GithubException.ErrorType.VALIDATION);
	}

	@Test
	void validate_whenOwnerMissing_shouldThrowValidationError() {
		var command = new CreatePullRequestCommand(null, "repo", "main", "feature/x", "Title", null);

		GithubException error = catchThrowableOfType(command::validate, GithubException.class);

		assertThat(error.errorType()).isEqualTo(GithubException.ErrorType.VALIDATION);
	}
}
