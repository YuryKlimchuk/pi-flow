package com.hydroyura.agents.piflow.github.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import com.hydroyura.agents.piflow.github.domain.GithubBranchRef;
import com.hydroyura.agents.piflow.github.domain.GithubCreatePullRequest;
import com.hydroyura.agents.piflow.github.domain.GithubIssueFilter;
import com.hydroyura.agents.piflow.github.domain.GithubPage;
import com.hydroyura.agents.piflow.github.domain.GithubPullRequest;
import com.hydroyura.agents.piflow.github.domain.GithubRemoteException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class RestGithubApiAdapterOperationsTest {

	private StubHttpServer server;

	@BeforeEach
	void setUp() {
		server = StubHttpServer.start();
	}

	@AfterEach
	void tearDown() {
		server.close();
	}

	@Test
	void listRepositories_whenFirstPage_shouldMapItems() {
		server.stub("GET", "/user/repos", StubHttpServer.StubResponse.json(200,
				"[{\"id\":1,\"owner\":{\"login\":\"acme\"},\"name\":\"repo\",\"html_url\":\"https://github.com/acme/repo\"}]"));

		GithubPage<com.hydroyura.agents.piflow.github.domain.GithubRepository> result = adapter().listRepositories(1, 30);

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).owner()).isEqualTo("acme");
		assertThat(result.items().get(0).name()).isEqualTo("repo");
		assertThat(result.hasNext()).isFalse();
	}

	@Test
	void listRepositories_whenLinkHeaderHasNext_shouldReturnHasNextTrue() {
		Map<String, String> headers = Map.of("Link", "<https://api.github.com/user/repos?page=2>; rel=\"next\"");
		server.stub("GET", "/user/repos", StubHttpServer.StubResponse.json(200, "[]", headers));

		GithubPage<com.hydroyura.agents.piflow.github.domain.GithubRepository> result = adapter().listRepositories(1, 30);

		assertThat(result.hasNext()).isTrue();
	}

	@Test
	void listIssues_whenItemIsPullRequest_shouldMarkPullRequestTrue() {
		server.stub("GET", "/repos/acme/repo/issues", StubHttpServer.StubResponse.json(200,
				"[{\"number\":8,\"title\":\"PR\",\"state\":\"open\",\"body\":null,"
						+ "\"user\":{\"login\":\"bob\"},\"html_url\":\"https://github.com/acme/repo/pull/8\","
						+ "\"pull_request\":{\"url\":\"https://api.github.com/repos/acme/repo/pulls/8\"}}]"));

		var result = adapter().listIssues("acme", "repo", GithubIssueFilter.ALL, 1, 30);

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).pullRequest()).isTrue();
	}

	@Test
	void listIssues_whenItemIsPlainIssue_shouldMarkPullRequestFalse() {
		server.stub("GET", "/repos/acme/repo/issues", StubHttpServer.StubResponse.json(200,
				"[{\"number\":7,\"title\":\"Bug\",\"state\":\"open\",\"body\":null,"
						+ "\"user\":{\"login\":\"alice\"},\"html_url\":\"https://github.com/acme/repo/issues/7\","
						+ "\"pull_request\":null}]"));

		var result = adapter().listIssues("acme", "repo", GithubIssueFilter.ALL, 1, 30);

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).number()).isEqualTo(7L);
		assertThat(result.items().get(0).pullRequest()).isFalse();
	}

	@Test
	void getIssue_whenFound_shouldMapDetails() {
		server.stub("GET", "/repos/acme/repo/issues/7", StubHttpServer.StubResponse.json(200,
				"{\"number\":7,\"title\":\"Bug\",\"state\":\"open\",\"body\":\"Description\","
						+ "\"user\":{\"login\":\"alice\"},\"html_url\":\"https://github.com/acme/repo/issues/7\","
						+ "\"pull_request\":null}"));

		var result = adapter().getIssue("acme", "repo", 7L);

		assertThat(result.number()).isEqualTo(7L);
		assertThat(result.description()).isEqualTo("Description");
	}

	@Test
	void getIssue_whenNotFound_shouldThrowNotFound() {
		server.stub("GET", "/repos/acme/repo/issues/42", StubHttpServer.StubResponse.json(404, "{}"));

		GithubRemoteException error = org.assertj.core.api.Assertions
				.catchThrowableOfType(() -> adapter().getIssue("acme", "repo", 42L), GithubRemoteException.class);

		assertThat(error.errorType()).isEqualTo(GithubRemoteException.ErrorType.NOT_FOUND);
	}

	@Test
	void publishBranch_whenBranchExists_shouldReturnRefAndUrl() {
		server.stub("GET", "/repos/acme/repo/branches/feature/x", StubHttpServer.StubResponse.json(200,
				"{\"name\":\"feature/x\"}"));

		GithubBranchRef result = adapter().publishBranch("acme", "repo", "feature/x");

		assertThat(result.ref()).isEqualTo("refs/heads/feature/x");
		assertThat(result.url()).isEqualTo("https://github.com/acme/repo/tree/feature/x");
	}

	@Test
	void createPullRequest_whenExistingOpenPr_shouldReturnItWithoutPost() {
		server.stub("GET", "/repos/acme/repo/pulls", StubHttpServer.StubResponse.json(200,
				"[{\"number\":11,\"state\":\"open\",\"html_url\":\"https://github.com/acme/repo/pull/11\","
						+ "\"head\":{\"ref\":\"feature/x\"},\"base\":{\"ref\":\"main\"}}]"));

		GithubPullRequest result = adapter().createPullRequest(command());

		assertThat(result.number()).isEqualTo(11L);
		assertThat(result.url()).isEqualTo("https://github.com/acme/repo/pull/11");
		assertThat(server.requests().stream().filter(r -> "POST".equals(r.method()))).isEmpty();
	}

	@Test
	void createPullRequest_whenNoExistingPr_shouldPostAndMap() {
		server.stub("GET", "/repos/acme/repo/pulls", StubHttpServer.StubResponse.json(200, "[]"));
		server.stub("POST", "/repos/acme/repo/pulls", StubHttpServer.StubResponse.json(201,
				"{\"number\":12,\"state\":\"open\",\"html_url\":\"https://github.com/acme/repo/pull/12\","
						+ "\"head\":{\"ref\":\"feature/x\"},\"base\":{\"ref\":\"main\"}}"));

		GithubPullRequest result = adapter().createPullRequest(command());

		assertThat(result.number()).isEqualTo(12L);
		assertThat(result.url()).isEqualTo("https://github.com/acme/repo/pull/12");
		assertThat(server.requests().stream().filter(r -> "POST".equals(r.method()))).hasSize(1);
	}

	@Test
	void createPullRequest_whenRaceConflict_shouldRetrySearchAndReturnExisting() {
		server.stub("GET", "/repos/acme/repo/pulls",
				StubHttpServer.StubResponse.json(200, "[]"),
				StubHttpServer.StubResponse.json(200,
						"[{\"number\":13,\"state\":\"open\",\"html_url\":\"https://github.com/acme/repo/pull/13\","
								+ "\"head\":{\"ref\":\"feature/x\"},\"base\":{\"ref\":\"main\"}}]"));
		server.stub("POST", "/repos/acme/repo/pulls", StubHttpServer.StubResponse.json(409, "{}"));

		GithubPullRequest result = adapter().createPullRequest(command());

		assertThat(result.number()).isEqualTo(13L);
		assertThat(server.requests().stream().filter(r -> "GET".equals(r.method()))).hasSize(2);
		assertThat(server.requests().stream().filter(r -> "POST".equals(r.method()))).hasSize(1);
	}

	@Test
	void createPullRequest_whenOnlyClosedPr_shouldCreateNewOne() {
		server.stub("GET", "/repos/acme/repo/pulls", StubHttpServer.StubResponse.json(200,
				"[{\"number\":9,\"state\":\"closed\",\"html_url\":\"https://github.com/acme/repo/pull/9\","
						+ "\"head\":{\"ref\":\"feature/x\"},\"base\":{\"ref\":\"main\"}}]"));
		server.stub("POST", "/repos/acme/repo/pulls", StubHttpServer.StubResponse.json(201,
				"{\"number\":14,\"state\":\"open\",\"html_url\":\"https://github.com/acme/repo/pull/14\","
						+ "\"head\":{\"ref\":\"feature/x\"},\"base\":{\"ref\":\"main\"}}"));

		GithubPullRequest result = adapter().createPullRequest(command());

		assertThat(result.number()).isEqualTo(14L);
		assertThat(server.requests().stream().filter(r -> "POST".equals(r.method()))).hasSize(1);
	}

	private GithubCreatePullRequest command() {
		return new GithubCreatePullRequest("acme", "repo", "main", "feature/x", "Title", "Body");
	}

	private RestGithubApiAdapter adapter() {
		GithubApiProperties properties = new GithubApiProperties(server.baseUrl(), "secret-token", 30,
				Duration.ofSeconds(5), Duration.ofSeconds(5));
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(5));
		factory.setReadTimeout(Duration.ofSeconds(5));
		RestClient client = RestClient.builder().baseUrl(server.baseUrl()).requestFactory(factory).build();
		return new RestGithubApiAdapter(client, properties);
	}
}
