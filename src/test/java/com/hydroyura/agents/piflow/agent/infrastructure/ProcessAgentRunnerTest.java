package com.hydroyura.agents.piflow.agent.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.hydroyura.agents.piflow.agent.FakePi;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessException;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessRequest;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessResult;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessAgentRunnerTest {

	private static final String JAVA = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
	private static final String CLASSPATH = System.getProperty("surefire.test.class.path",
			System.getProperty("java.class.path"));

	private Path workDir;
	private ExecutorService executor;

	@BeforeEach
	void setUp() throws IOException {
		workDir = Files.createTempDirectory("agent-test");
		executor = Executors.newSingleThreadExecutor();
	}

	@AfterEach
	void tearDown() {
		executor.shutdownNow();
	}

	@Test
	void execute_whenExitZero_shouldReturnSucceededWithStdout() throws Exception {
		ProcessAgentRunner runner = runner(defaultProperties(), Map.of());

		AgentProcessResult result = runner.execute(request(Map.of(), null));

		assertThat(result.status()).isEqualTo(AgentProcessStatus.SUCCEEDED);
		assertThat(result.exitCode()).isZero();
		assertThat(result.stdout()).contains("STDIN:do something");
		assertThat(result.stdout()).contains("--role");
	}

	@Test
	void execute_whenNonZeroExit_shouldReturnFailedWithExitCode() {
		ProcessAgentRunner runner = runner(defaultProperties(), Map.of());

		AgentProcessResult result = runner.execute(request(Map.of("exit-code", "7"), null));

		assertThat(result.status()).isEqualTo(AgentProcessStatus.FAILED);
		assertThat(result.exitCode()).isEqualTo(7);
	}

	@Test
	void execute_whenExecutableMissing_shouldThrowLaunchError() {
		AgentProperties properties = new AgentProperties(workDir.resolve("missing-pi").toString(), List.of(), List.of(),
				Duration.ofSeconds(5), Map.of());
		ProcessAgentRunner runner = runner(properties, Map.of());

		AgentProcessException error = catchThrowableOfType(() -> runner.execute(request(Map.of(), null)),
				AgentProcessException.class);

		assertThat(error.errorType()).isEqualTo(AgentProcessException.ErrorType.LAUNCH);
	}

	@Test
	void execute_whenBigOutput_shouldDrainBothStreams() {
		ProcessAgentRunner runner = runner(defaultProperties(), Map.of());

		AgentProcessResult result = runner.execute(request(Map.of("big", "true"), null));

		assertThat(result.status()).isEqualTo(AgentProcessStatus.SUCCEEDED);
		assertThat(result.stdout()).hasSizeGreaterThan(64 * 1024);
		assertThat(result.stderr()).hasSizeGreaterThan(64 * 1024);
	}

	@Test
	void execute_whenTimeoutExceeded_shouldReturnTimedOut() {
		ProcessAgentRunner runner = runner(defaultProperties(), Map.of());

		AgentProcessResult result = runner.execute(request(Map.of("hang", "true"), Duration.ofMillis(500)));

		assertThat(result.status()).isEqualTo(AgentProcessStatus.TIMED_OUT);
	}

	@Test
	void cancel_whenRunActive_shouldReturnCancelled() throws Exception {
		ProcessAgentRunner runner = runner(defaultProperties(), Map.of());
		String runId = "run-1";
		Future<AgentProcessResult> future = executor.submit(() -> runner.execute(request(runId, Map.of("hang", "true"), null)));

		Thread.sleep(500);
		runner.cancel(runId);

		AgentProcessResult result = future.get(10, TimeUnit.SECONDS);
		assertThat(result.status()).isEqualTo(AgentProcessStatus.CANCELLED);
	}

	@Test
	void execute_whenAllowlistedSecret_shouldPassOnlyAllowedEnv() {
		AgentProperties properties = new AgentProperties(JAVA, List.of("-cp", CLASSPATH, FakePi.class.getName()),
				List.of("SECRET_VAR"), Duration.ofSeconds(5), Map.of());
		ProcessAgentRunner runner = runner(properties, Map.of("SECRET_VAR", "hunter2", "OTHER", "other-value"));

		AgentProcessResult result = runner.execute(request(Map.of(), null));

		assertThat(result.stdout()).contains("ENV_SECRET:hunter2");
		assertThat(result.stdout()).contains("ENV_OTHER:null");
	}

	private AgentProperties defaultProperties() {
		return new AgentProperties(JAVA, List.of("-cp", CLASSPATH, FakePi.class.getName()), List.of(),
				Duration.ofSeconds(30), Map.of());
	}

	private ProcessAgentRunner runner(AgentProperties properties, Map<String, String> env) {
		return new ProcessAgentRunner(properties, name -> env.get(name));
	}

	private AgentProcessRequest request(Map<String, String> parameters, Duration timeout) {
		return request("run-id", parameters, timeout);
	}

	private AgentProcessRequest request(String runId, Map<String, String> parameters, Duration timeout) {
		return new AgentProcessRequest(runId, "ANALYST", "do something", "input", "context", workDir, parameters,
				timeout);
	}
}
