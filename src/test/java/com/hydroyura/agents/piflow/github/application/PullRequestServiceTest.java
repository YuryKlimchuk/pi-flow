package com.hydroyura.agents.piflow.github.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hydroyura.agents.piflow.github.api.CreatePullRequestCommand;
import com.hydroyura.agents.piflow.github.api.GithubException;
import com.hydroyura.agents.piflow.github.api.PullRequestResult;
import com.hydroyura.agents.piflow.github.domain.GithubApi;
import com.hydroyura.agents.piflow.github.domain.GithubPullRequest;
import com.hydroyura.agents.piflow.github.domain.GithubRemoteException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PullRequestServiceTest {

	@Mock
	private GithubApi githubApi;

	@InjectMocks
	private PullRequestService service;

	@Test
	void createPullRequest_whenValid_shouldDelegateAndMap() {
		var command = new CreatePullRequestCommand("acme", "repo", "main", "feature/x", "Title", "Body");
		when(githubApi.createPullRequest(org.mockito.ArgumentMatchers.any())).thenReturn(new GithubPullRequest(11L,
				"https://github.com/acme/repo/pull/11"));

		PullRequestResult result = service.createPullRequest(command);

		assertThat(result.number()).isEqualTo(11L);
		assertThat(result.url()).isEqualTo("https://github.com/acme/repo/pull/11");
		verify(githubApi).createPullRequest(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void createPullRequest_whenTitleBlank_shouldThrowWithoutCallingApi() {
		var command = new CreatePullRequestCommand("acme", "repo", "main", "feature/x", "", "Body");

		GithubException error = catchThrowableOfType(() -> service.createPullRequest(command), GithubException.class);

		assertThat(error.errorType()).isEqualTo(GithubException.ErrorType.VALIDATION);
		verifyNoInteractions(githubApi);
	}

	@Test
	void createPullRequest_whenConflict_shouldMapToConflictError() {
		var command = new CreatePullRequestCommand("acme", "repo", "main", "feature/x", "Title", "Body");
		when(githubApi.createPullRequest(org.mockito.ArgumentMatchers.any()))
				.thenThrow(GithubRemoteException.conflict("createPullRequest", 409));

		GithubException error = catchThrowableOfType(() -> service.createPullRequest(command), GithubException.class);

		assertThat(error.errorType()).isEqualTo(GithubException.ErrorType.CONFLICT);
		assertThat(error.status()).isEqualTo(409);
	}
}
