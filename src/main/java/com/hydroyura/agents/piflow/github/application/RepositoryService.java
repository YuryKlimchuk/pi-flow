package com.hydroyura.agents.piflow.github.application;

import com.hydroyura.agents.piflow.github.api.PageRequest;
import com.hydroyura.agents.piflow.github.api.RepositoryOperations;
import com.hydroyura.agents.piflow.github.api.RepositoryPage;
import com.hydroyura.agents.piflow.github.api.RepositorySummary;
import com.hydroyura.agents.piflow.github.domain.GithubApi;
import com.hydroyura.agents.piflow.github.domain.GithubRemoteException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Реализует публичный порт получения репозиториев через внутренний порт GitHub API.
 */
@Service
@RequiredArgsConstructor
public class RepositoryService implements RepositoryOperations {

	private final GithubApi githubApi;

	@Override
	public RepositoryPage listRepositories(PageRequest page) {
		PageValidator.validate(page);
		try {
			var result = githubApi.listRepositories(page.page(), page.perPage());
			var items = result.items().stream().map(this::toSummary).toList();
			return new RepositoryPage(items, result.page(), result.perPage(), result.hasNext());
		} catch (GithubRemoteException e) {
			throw GithubErrors.map(e);
		}
	}

	private RepositorySummary toSummary(com.hydroyura.agents.piflow.github.domain.GithubRepository repository) {
		return new RepositorySummary(repository.id(), repository.owner(), repository.name(), repository.url());
	}
}
