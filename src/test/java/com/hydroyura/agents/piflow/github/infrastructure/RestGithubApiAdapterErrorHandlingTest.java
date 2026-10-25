package com.hydroyura.agents.piflow.github.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Duration;

import com.hydroyura.agents.piflow.github.domain.GithubRemoteException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class RestGithubApiAdapterErrorHandlingTest {

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
	void listRepositories_whenTokenPresent_shouldSendAuthorizationHeader() {
		server.stub("GET", "/user/repos", StubHttpServer.StubResponse.json(200, "[]"));
		var adapter = adapter("secret-token", Duration.ofSeconds(5));

		adapter.listRepositories(1, 30);

		assertThat(server.requests()).hasSize(1);
		assertThat(server.requests().get(0).authorization()).isEqualTo("Bearer secret-token");
	}

	@Test
	void listRepositories_whenTokenMissing_shouldFailWithoutSendingRequest() {
		var adapter = adapter(null, Duration.ofSeconds(5));

		GithubRemoteException error = catchThrowableOfType(() -> adapter.listRepositories(1, 30),
				GithubRemoteException.class);

		assertThat(error.errorType()).isEqualTo(GithubRemoteException.ErrorType.CONFIGURATION);
		assertThat(server.requests()).isEmpty();
	}

	@Test
	void listRepositories_when401_shouldMapToAuthentication() {
		server.stub("GET", "/user/repos", StubHttpServer.StubResponse.json(401, "{}"));

		GithubRemoteException error = catchThrowableOfType(() -> adapter("t", Duration.ofSeconds(5)).listRepositories(1, 30),
				GithubRemoteException.class);

		assertThat(error.errorType()).isEqualTo(GithubRemoteException.ErrorType.AUTHENTICATION);
		assertThat(error.status()).isEqualTo(401);
		assertThat(error.retryable()).isFalse();
	}

	@Test
	void listRepositories_when403_shouldMapToAuthentication() {
		server.stub("GET", "/user/repos", StubHttpServer.StubResponse.json(403, "{}"));

		GithubRemoteException error = catchThrowableOfType(() -> adapter("t", Duration.ofSeconds(5)).listRepositories(1, 30),
				GithubRemoteException.class);

		assertThat(error.errorType()).isEqualTo(GithubRemoteException.ErrorType.AUTHENTICATION);
		assertThat(error.status()).isEqualTo(403);
	}

	@Test
	void listRepositories_when404_shouldMapToNotFound() {
		server.stub("GET", "/user/repos", StubHttpServer.StubResponse.json(404, "{}"));

		GithubRemoteException error = catchThrowableOfType(() -> adapter("t", Duration.ofSeconds(5)).listRepositories(1, 30),
				GithubRemoteException.class);

		assertThat(error.errorType()).isEqualTo(GithubRemoteException.ErrorType.NOT_FOUND);
		assertThat(error.status()).isEqualTo(404);
	}

	@Test
	void listRepositories_when409_shouldMapToConflict() {
		server.stub("GET", "/user/repos", StubHttpServer.StubResponse.json(409, "{}"));

		GithubRemoteException error = catchThrowableOfType(() -> adapter("t", Duration.ofSeconds(5)).listRepositories(1, 30),
				GithubRemoteException.class);

		assertThat(error.errorType()).isEqualTo(GithubRemoteException.ErrorType.CONFLICT);
		assertThat(error.status()).isEqualTo(409);
	}

	@Test
	void listRepositories_when422_shouldMapToValidation() {
		server.stub("GET", "/user/repos", StubHttpServer.StubResponse.json(422, "{}"));

		GithubRemoteException error = catchThrowableOfType(() -> adapter("t", Duration.ofSeconds(5)).listRepositories(1, 30),
				GithubRemoteException.class);

		assertThat(error.errorType()).isEqualTo(GithubRemoteException.ErrorType.VALIDATION);
		assertThat(error.status()).isEqualTo(422);
	}

	@Test
	void listRepositories_when429_shouldMapToRetryableRateLimit() {
		server.stub("GET", "/user/repos", StubHttpServer.StubResponse.json(429, "{}"));

		GithubRemoteException error = catchThrowableOfType(() -> adapter("t", Duration.ofSeconds(5)).listRepositories(1, 30),
				GithubRemoteException.class);

		assertThat(error.errorType()).isEqualTo(GithubRemoteException.ErrorType.RATE_LIMIT);
		assertThat(error.status()).isEqualTo(429);
		assertThat(error.retryable()).isTrue();
	}

	@Test
	void listRepositories_when500_shouldMapToRetryableServerError() {
		server.stub("GET", "/user/repos", StubHttpServer.StubResponse.json(500, "{}"));

		GithubRemoteException error = catchThrowableOfType(() -> adapter("t", Duration.ofSeconds(5)).listRepositories(1, 30),
				GithubRemoteException.class);

		assertThat(error.errorType()).isEqualTo(GithubRemoteException.ErrorType.SERVER);
		assertThat(error.status()).isEqualTo(500);
		assertThat(error.retryable()).isTrue();
	}

	@Test
	void listRepositories_whenTimeout_shouldMapToRetryableNetworkError() {
		server.stub("GET", "/user/repos", StubHttpServer.StubResponse.delayed(200, "[]", 2000));

		GithubRemoteException error = catchThrowableOfType(
				() -> adapter("t", Duration.ofMillis(200)).listRepositories(1, 30), GithubRemoteException.class);

		assertThat(error.errorType()).isEqualTo(GithubRemoteException.ErrorType.NETWORK);
		assertThat(error.retryable()).isTrue();
	}

	private RestGithubApiAdapter adapter(String token, Duration readTimeout) {
		GithubApiProperties properties = new GithubApiProperties(server.baseUrl(), token, 30, Duration.ofSeconds(5),
				readTimeout);
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(5));
		factory.setReadTimeout(readTimeout);
		RestClient client = RestClient.builder().baseUrl(server.baseUrl()).requestFactory(factory).build();
		return new RestGithubApiAdapter(client, properties);
	}
}
