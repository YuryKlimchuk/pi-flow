package com.hydroyura.agents.piflow.github.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import com.hydroyura.agents.piflow.github.api.GithubException;
import com.hydroyura.agents.piflow.github.api.PageRequest;
import com.hydroyura.agents.piflow.github.api.RepositoryPage;
import com.hydroyura.agents.piflow.github.domain.GithubApi;
import com.hydroyura.agents.piflow.github.domain.GithubPage;
import com.hydroyura.agents.piflow.github.domain.GithubRemoteException;
import com.hydroyura.agents.piflow.github.domain.GithubRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {

	@Mock
	private GithubApi githubApi;

	@InjectMocks
	private RepositoryService service;

	@Test
	void listRepositories_whenPageValid_shouldDelegateAndMap() {
		var domain = new GithubRepository(1L, "acme", "repo", "https://github.com/acme/repo");
		when(githubApi.listRepositories(1, 30)).thenReturn(GithubPage.of(List.of(domain), 1, 30, false));

		RepositoryPage result = service.listRepositories(PageRequest.of(1, 30));

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).owner()).isEqualTo("acme");
		assertThat(result.items().get(0).url()).isEqualTo("https://github.com/acme/repo");
		assertThat(result.hasNext()).isFalse();
		verify(githubApi).listRepositories(1, 30);
	}

	@Test
	void listRepositories_whenInvalidPage_shouldThrowWithoutCallingApi() {
		GithubException error = catchThrowableOfType(() -> service.listRepositories(PageRequest.of(0, 30)),
				GithubException.class);

		assertThat(error.errorType()).isEqualTo(GithubException.ErrorType.VALIDATION);
		verifyNoInteractions(githubApi);
	}

	@Test
	void listRepositories_whenRateLimited_shouldMapToRetryableError() {
		when(githubApi.listRepositories(1, 30)).thenThrow(GithubRemoteException.rateLimit("listRepositories", 429));

		GithubException error = catchThrowableOfType(() -> service.listRepositories(PageRequest.of(1, 30)),
				GithubException.class);

		assertThat(error.errorType()).isEqualTo(GithubException.ErrorType.RATE_LIMIT);
		assertThat(error.status()).isEqualTo(429);
		assertThat(error.retryable()).isTrue();
	}
}
