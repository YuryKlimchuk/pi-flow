package com.hydroyura.agents.piflow.github.application;

import com.hydroyura.agents.piflow.github.api.CreatePullRequestCommand;
import com.hydroyura.agents.piflow.github.api.PullRequestOperations;
import com.hydroyura.agents.piflow.github.api.PullRequestResult;
import com.hydroyura.agents.piflow.github.domain.GithubApi;
import com.hydroyura.agents.piflow.github.domain.GithubCreatePullRequest;
import com.hydroyura.agents.piflow.github.domain.GithubRemoteException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Реализует публичный порт создания pull request.
 */
@Service
@RequiredArgsConstructor
public class PullRequestService implements PullRequestOperations {

	private final GithubApi githubApi;

	@Override
	public PullRequestResult createPullRequest(CreatePullRequestCommand command) {
		command.validate();
		try {
			var result = githubApi.createPullRequest(toDomain(command));
			return new PullRequestResult(result.number(), result.url());
		} catch (GithubRemoteException e) {
			throw GithubErrors.map(e);
		}
	}

	private GithubCreatePullRequest toDomain(CreatePullRequestCommand command) {
		return new GithubCreatePullRequest(command.owner(), command.repository(), command.base(), command.head(),
				command.title(), command.description());
	}
}
