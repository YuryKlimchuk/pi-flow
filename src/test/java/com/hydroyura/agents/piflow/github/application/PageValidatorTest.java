package com.hydroyura.agents.piflow.github.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.hydroyura.agents.piflow.github.api.GithubException;
import com.hydroyura.agents.piflow.github.api.PageRequest;
import org.junit.jupiter.api.Test;

class PageValidatorTest {

	@Test
	void validate_whenPageAndPerPageValid_shouldPass() {
		Throwable error = catchThrowable(() -> PageValidator.validate(PageRequest.of(1, 30)));

		assertThat(error).isNull();
	}

	@Test
	void validate_whenPageZero_shouldThrowValidationError() {
		GithubException error = catchThrowableOfType(() -> PageValidator.validate(PageRequest.of(0, 30)),
				GithubException.class);

		assertThat(error.errorType()).isEqualTo(GithubException.ErrorType.VALIDATION);
	}

	@Test
	void validate_whenPerPageZero_shouldThrowValidationError() {
		GithubException error = catchThrowableOfType(() -> PageValidator.validate(PageRequest.of(1, 0)),
				GithubException.class);

		assertThat(error.errorType()).isEqualTo(GithubException.ErrorType.VALIDATION);
	}

	@Test
	void validate_whenPerPageTooLarge_shouldThrowValidationError() {
		GithubException error = catchThrowableOfType(() -> PageValidator.validate(PageRequest.of(1, 101)),
				GithubException.class);

		assertThat(error.errorType()).isEqualTo(GithubException.ErrorType.VALIDATION);
	}
}
