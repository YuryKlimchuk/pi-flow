package com.hydroyura.agents.piflow.github.application;

import com.hydroyura.agents.piflow.github.api.IssueDetails;
import com.hydroyura.agents.piflow.github.api.IssueFilter;
import com.hydroyura.agents.piflow.github.api.IssueOperations;
import com.hydroyura.agents.piflow.github.api.IssuePage;
import com.hydroyura.agents.piflow.github.api.IssueState;
import com.hydroyura.agents.piflow.github.api.IssueSummary;
import com.hydroyura.agents.piflow.github.api.PageRequest;
import com.hydroyura.agents.piflow.github.domain.GithubApi;
import com.hydroyura.agents.piflow.github.domain.GithubIssue;
import com.hydroyura.agents.piflow.github.domain.GithubIssueDetails;
import com.hydroyura.agents.piflow.github.domain.GithubIssueFilter;
import com.hydroyura.agents.piflow.github.domain.GithubIssueState;
import com.hydroyura.agents.piflow.github.domain.GithubRemoteException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Реализует публичный порт получения задач (Issues) через внутренний порт GitHub API.
 */
@Service
@RequiredArgsConstructor
public class IssueService implements IssueOperations {

	private final GithubApi githubApi;

	@Override
	public IssuePage listIssues(String owner, String repository, IssueFilter filter, PageRequest page) {
		PageValidator.validate(page);
		try {
			var domainFilter = toDomainFilter(filter);
			var result = githubApi.listIssues(owner, repository, domainFilter, page.page(), page.perPage());
			var items = result.items().stream().map(this::toSummary).toList();
			return new IssuePage(items, result.page(), result.perPage(), result.hasNext());
		} catch (GithubRemoteException e) {
			throw GithubErrors.map(e);
		}
	}

	@Override
	public IssueDetails getIssue(String owner, String repository, long number) {
		try {
			return toDetails(githubApi.getIssue(owner, repository, number));
		} catch (GithubRemoteException e) {
			throw GithubErrors.map(e);
		}
	}

	private GithubIssueFilter toDomainFilter(IssueFilter filter) {
		if (filter == null || filter.state() == null) {
			return GithubIssueFilter.ALL;
		}
		return new GithubIssueFilter(toDomainState(filter.state()));
	}

	private IssueSummary toSummary(GithubIssue issue) {
		return new IssueSummary(issue.number(), issue.title(), toPublicState(issue.state()), issue.author(), issue.url(),
				issue.pullRequest());
	}

	private IssueDetails toDetails(GithubIssueDetails issue) {
		return new IssueDetails(issue.number(), issue.title(), issue.description(), toPublicState(issue.state()),
				issue.author(), issue.url(), issue.pullRequest());
	}

	private IssueState toPublicState(GithubIssueState state) {
		return state == GithubIssueState.OPEN ? IssueState.OPEN : IssueState.CLOSED;
	}

	private GithubIssueState toDomainState(IssueState state) {
		return state == IssueState.OPEN ? GithubIssueState.OPEN : GithubIssueState.CLOSED;
	}
}
