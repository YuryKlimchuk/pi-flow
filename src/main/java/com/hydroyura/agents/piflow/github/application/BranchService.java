package com.hydroyura.agents.piflow.github.application;

import com.hydroyura.agents.piflow.github.api.BranchOperations;
import com.hydroyura.agents.piflow.github.api.BranchPublication;
import com.hydroyura.agents.piflow.github.domain.GithubApi;
import com.hydroyura.agents.piflow.github.domain.GithubRemoteException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Реализует публичный порт публикации подготовленной ветки.
 */
@Service
@RequiredArgsConstructor
public class BranchService implements BranchOperations {

	private final GithubApi githubApi;

	@Override
	public BranchPublication publishBranch(String owner, String repository, String branch) {
		try {
			var ref = githubApi.publishBranch(owner, repository, branch);
			return new BranchPublication(ref.ref(), ref.url());
		} catch (GithubRemoteException e) {
			throw GithubErrors.map(e);
		}
	}
}
