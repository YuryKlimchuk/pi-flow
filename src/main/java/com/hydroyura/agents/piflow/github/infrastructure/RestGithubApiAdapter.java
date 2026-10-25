package com.hydroyura.agents.piflow.github.infrastructure;

import java.io.IOException;
import java.util.List;

import com.hydroyura.agents.piflow.github.domain.GithubApi;
import com.hydroyura.agents.piflow.github.domain.GithubBranchRef;
import com.hydroyura.agents.piflow.github.domain.GithubCreatePullRequest;
import com.hydroyura.agents.piflow.github.domain.GithubIssue;
import com.hydroyura.agents.piflow.github.domain.GithubIssueDetails;
import com.hydroyura.agents.piflow.github.domain.GithubIssueFilter;
import com.hydroyura.agents.piflow.github.domain.GithubIssueState;
import com.hydroyura.agents.piflow.github.domain.GithubPage;
import com.hydroyura.agents.piflow.github.domain.GithubPullRequest;
import com.hydroyura.agents.piflow.github.domain.GithubRemoteException;
import com.hydroyura.agents.piflow.github.domain.GithubRemoteException.ErrorType;
import com.hydroyura.agents.piflow.github.domain.GithubRepository;
import com.hydroyura.agents.piflow.github.infrastructure.dto.BranchDto;
import com.hydroyura.agents.piflow.github.infrastructure.dto.CreatePullRequestDto;
import com.hydroyura.agents.piflow.github.infrastructure.dto.IssueDto;
import com.hydroyura.agents.piflow.github.infrastructure.dto.PullRequestDto;
import com.hydroyura.agents.piflow.github.infrastructure.dto.RepoDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.util.UriBuilder;

/**
 * Адаптер GitHub REST API на базе {@link RestClient}.
 *
 * <p>Реализует внутренний порт {@link GithubApi} и инкапсулирует HTTP-детали,
 * заголовки, пагинацию и классификацию ошибок. Локальные Git-операции не выполняет.</p>
 */
@Component
public class RestGithubApiAdapter implements GithubApi {

	private static final String AUTHORIZATION = HttpHeaders.AUTHORIZATION;

	private final RestClient restClient;
	private final GithubApiProperties properties;

	public RestGithubApiAdapter(RestClient githubRestClient, GithubApiProperties properties) {
		this.restClient = githubRestClient;
		this.properties = properties;
	}

	@Override
	public GithubPage<GithubRepository> listRepositories(int page, int perPage) {
		requireToken("listRepositories");
		try {
			return restClient.get()
					.uri(b -> b.path("/user/repos")
							.queryParam("page", page)
							.queryParam("per_page", perPage)
							.build())
					.header(AUTHORIZATION, bearerToken())
					.exchange((request, response) -> {
						throwOnError(response, "listRepositories");
						List<RepoDto> dtos = response.bodyTo(new ParameterizedTypeReference<List<RepoDto>>() {
						});
						boolean hasNext = resolveHasNext(response.getHeaders(), dtos.size(), perPage);
						return GithubPage.of(dtos.stream().map(this::toRepository).toList(), page, perPage, hasNext);
					});
		} catch (GithubRemoteException e) {
			throw e;
		} catch (ResourceAccessException e) {
			throw GithubRemoteException.network("listRepositories", e);
		}
	}

	@Override
	public GithubPage<GithubIssue> listIssues(String owner, String repository, GithubIssueFilter filter, int page,
			int perPage) {
		requireToken("listIssues");
		try {
			return restClient.get()
					.uri(b -> {
						UriBuilder builder = b.path("/repos/{owner}/{repo}/issues")
								.queryParam("page", page)
								.queryParam("per_page", perPage);
						if (filter != null && filter.state() != null) {
							builder = builder.queryParam("state", filter.state() == GithubIssueState.OPEN ? "open" : "closed");
						}
						return builder.build(owner, repository);
					})
					.header(AUTHORIZATION, bearerToken())
					.exchange((request, response) -> {
						throwOnError(response, "listIssues");
						List<IssueDto> dtos = response.bodyTo(new ParameterizedTypeReference<List<IssueDto>>() {
						});
						boolean hasNext = resolveHasNext(response.getHeaders(), dtos.size(), perPage);
						return GithubPage.of(dtos.stream().map(this::toIssue).toList(), page, perPage, hasNext);
					});
		} catch (GithubRemoteException e) {
			throw e;
		} catch (ResourceAccessException e) {
			throw GithubRemoteException.network("listIssues", e);
		}
	}

	@Override
	public GithubIssueDetails getIssue(String owner, String repository, long number) {
		requireToken("getIssue");
		try {
			return restClient.get()
					.uri(b -> b.path("/repos/{owner}/{repo}/issues/{number}").build(owner, repository, number))
					.header(AUTHORIZATION, bearerToken())
					.exchange((request, response) -> {
						throwOnError(response, "getIssue");
						return toIssueDetails(response.bodyTo(IssueDto.class));
					});
		} catch (GithubRemoteException e) {
			throw e;
		} catch (ResourceAccessException e) {
			throw GithubRemoteException.network("getIssue", e);
		}
	}

	@Override
	public GithubBranchRef publishBranch(String owner, String repository, String branch) {
		requireToken("publishBranch");
		try {
			return restClient.get()
					.uri(b -> b.path("/repos/{owner}/{repo}/branches/{branch}").build(owner, repository, branch))
					.header(AUTHORIZATION, bearerToken())
					.exchange((request, response) -> {
						throwOnError(response, "publishBranch");
						response.bodyTo(BranchDto.class);
						return new GithubBranchRef("refs/heads/" + branch, webBranchUrl(owner, repository, branch));
					});
		} catch (GithubRemoteException e) {
			throw e;
		} catch (ResourceAccessException e) {
			throw GithubRemoteException.network("publishBranch", e);
		}
	}

	@Override
	public GithubPullRequest createPullRequest(GithubCreatePullRequest command) {
		requireToken("createPullRequest");

		GithubPullRequest existing = findOpenPullRequest(command.owner(), command.repository(), command.head(),
				command.base());
		if (existing != null) {
			return existing;
		}

		try {
			return postPullRequest(command);
		} catch (GithubRemoteException e) {
			if (e.errorType() == ErrorType.CONFLICT || e.errorType() == ErrorType.VALIDATION) {
				GithubPullRequest appeared = findOpenPullRequest(command.owner(), command.repository(), command.head(),
						command.base());
				if (appeared != null) {
					return appeared;
				}
			}
			throw e;
		}
	}

	private GithubPullRequest findOpenPullRequest(String owner, String repository, String head, String base) {
		try {
			return restClient.get()
					.uri(b -> b.path("/repos/{owner}/{repo}/pulls")
							.queryParam("state", "open")
							.queryParam("head", owner + ":" + head)
							.queryParam("base", base)
							.build(owner, repository))
					.header(AUTHORIZATION, bearerToken())
					.exchange((request, response) -> {
						throwOnError(response, "findOpenPullRequest");
						List<PullRequestDto> dtos = response
								.bodyTo(new ParameterizedTypeReference<List<PullRequestDto>>() {
								});
						return dtos.stream()
								.filter(d -> "open".equalsIgnoreCase(d.state()))
								.filter(d -> d.head() != null && head.equals(d.head().ref()))
								.filter(d -> d.base() != null && base.equals(d.base().ref()))
								.findFirst()
								.map(d -> new GithubPullRequest(d.number(), d.htmlUrl()))
								.orElse(null);
					});
		} catch (GithubRemoteException e) {
			throw e;
		} catch (ResourceAccessException e) {
			throw GithubRemoteException.network("findOpenPullRequest", e);
		}
	}

	private GithubPullRequest postPullRequest(GithubCreatePullRequest command) {
		try {
			return restClient.post()
					.uri(b -> b.path("/repos/{owner}/{repo}/pulls").build(command.owner(), command.repository()))
					.header(AUTHORIZATION, bearerToken())
					.contentType(MediaType.APPLICATION_JSON)
					.body(new CreatePullRequestDto(command.title(), command.description(), command.head(), command.base()))
					.exchange((request, response) -> {
						throwOnError(response, "createPullRequest");
						PullRequestDto dto = response.bodyTo(PullRequestDto.class);
						return new GithubPullRequest(dto.number(), dto.htmlUrl());
					});
		} catch (GithubRemoteException e) {
			throw e;
		} catch (ResourceAccessException e) {
			throw GithubRemoteException.network("createPullRequest", e);
		}
	}

	private void requireToken(String operation) {
		if (properties.token() == null || properties.token().isBlank()) {
			throw GithubRemoteException.configuration(operation);
		}
	}

	private String bearerToken() {
		return "Bearer " + properties.token();
	}

	private String webBranchUrl(String owner, String repository, String branch) {
		return "https://github.com/" + owner + "/" + repository + "/tree/" + branch;
	}

	private boolean resolveHasNext(HttpHeaders headers, int size, int perPage) {
		String link = headers.getFirst(HttpHeaders.LINK);
		if (link != null && link.contains("rel=\"next\"")) {
			return true;
		}
		return size >= perPage;
	}

	private static void throwOnError(ConvertibleClientHttpResponse response, String operation) throws IOException {
		HttpStatusCode status = response.getStatusCode();
		if (status.isError()) {
			throw mapStatusError(operation, status.value());
		}
	}

	private static GithubRemoteException mapStatusError(String operation, int status) {
		return switch (status) {
			case 401, 403 -> GithubRemoteException.authentication(operation, status);
			case 404 -> GithubRemoteException.notFound(operation, status);
			case 409 -> GithubRemoteException.conflict(operation, status);
			case 422 -> GithubRemoteException.validation(operation, status, "GitHub rejected the request (HTTP 422)");
			case 429 -> GithubRemoteException.rateLimit(operation, status);
			default -> status >= 500
					? GithubRemoteException.server(operation, status)
					: GithubRemoteException.validation(operation, status, "GitHub rejected the request (HTTP " + status + ")");
		};
	}

	private GithubRepository toRepository(RepoDto dto) {
		return new GithubRepository(dto.id(), dto.owner() != null ? dto.owner().login() : null, dto.name(),
				dto.htmlUrl());
	}

	private GithubIssue toIssue(IssueDto dto) {
		return new GithubIssue(dto.number(), dto.title(), parseState(dto.state()),
				dto.user() != null ? dto.user().login() : null, dto.htmlUrl(), dto.pullRequest() != null);
	}

	private GithubIssueDetails toIssueDetails(IssueDto dto) {
		return new GithubIssueDetails(dto.number(), dto.title(), dto.body(), parseState(dto.state()),
				dto.user() != null ? dto.user().login() : null, dto.htmlUrl(), dto.pullRequest() != null);
	}

	private GithubIssueState parseState(String state) {
		return "open".equalsIgnoreCase(state) ? GithubIssueState.OPEN : GithubIssueState.CLOSED;
	}
}
