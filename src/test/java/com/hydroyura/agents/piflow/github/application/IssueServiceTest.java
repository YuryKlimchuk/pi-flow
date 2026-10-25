package com.hydroyura.agents.piflow.github.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.hydroyura.agents.piflow.github.api.GithubException;
import com.hydroyura.agents.piflow.github.api.IssueDetails;
import com.hydroyura.agents.piflow.github.api.IssueFilter;
import com.hydroyura.agents.piflow.github.api.IssuePage;
import com.hydroyura.agents.piflow.github.api.IssueState;
import com.hydroyura.agents.piflow.github.api.PageRequest;
import com.hydroyura.agents.piflow.github.domain.GithubApi;
import com.hydroyura.agents.piflow.github.domain.GithubIssue;
import com.hydroyura.agents.piflow.github.domain.GithubIssueDetails;
import com.hydroyura.agents.piflow.github.domain.GithubIssueFilter;
import com.hydroyura.agents.piflow.github.domain.GithubIssueState;
import com.hydroyura.agents.piflow.github.domain.GithubPage;
import com.hydroyura.agents.piflow.github.domain.GithubRemoteException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

	@Mock
	private GithubApi githubApi;

	@InjectMocks
	private IssueService service;

	@Test
	void listIssues_whenValid_shouldDelegateAndMap() {
		var domain = new GithubIssue(7L, "Bug", GithubIssueState.OPEN, "alice", "https://github.com/acme/repo/issues/7",
				false);
		when(githubApi.listIssues("acme", "repo", GithubIssueFilter.ALL, 1, 20))
				.thenReturn(GithubPage.of(List.of(domain), 1, 20, true));

		IssuePage result = service.listIssues("acme", "repo", IssueFilter.ALL, PageRequest.of(1, 20));

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).number()).isEqualTo(7L);
		assertThat(result.items().get(0).pullRequest()).isFalse();
		assertThat(result.hasNext()).isTrue();
		verify(githubApi).listIssues("acme", "repo", GithubIssueFilter.ALL, 1, 20);
	}

	@Test
	void getIssue_whenFound_shouldMapDetails() {
		var domain = new GithubIssueDetails(7L, "Bug", "Description", GithubIssueState.OPEN, "alice",
				"https://github.com/acme/repo/issues/7", false);
		when(githubApi.getIssue("acme", "repo", 7L)).thenReturn(domain);

		IssueDetails result = service.getIssue("acme", "repo", 7L);

		assertThat(result.number()).isEqualTo(7L);
		assertThat(result.description()).isEqualTo("Description");
		assertThat(result.state()).isEqualTo(IssueState.OPEN);
		verify(githubApi).getIssue("acme", "repo", 7L);
	}

	@Test
	void getIssue_whenNotFound_shouldMapToNotFoundError() {
		when(githubApi.getIssue("acme", "repo", 42L)).thenThrow(GithubRemoteException.notFound("getIssue", 404));

		GithubException error = catchThrowableOfType(() -> service.getIssue("acme", "repo", 42L),
				GithubException.class);

		assertThat(error.errorType()).isEqualTo(GithubException.ErrorType.NOT_FOUND);
		assertThat(error.status()).isEqualTo(404);
	}
}
