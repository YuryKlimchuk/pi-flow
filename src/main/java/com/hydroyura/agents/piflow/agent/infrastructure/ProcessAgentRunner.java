package com.hydroyura.agents.piflow.agent.infrastructure;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.UnaryOperator;

import com.hydroyura.agents.piflow.agent.domain.AgentProcessException;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessRequest;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessResult;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessRunner;
import com.hydroyura.agents.piflow.agent.domain.AgentProcessStatus;
import com.hydroyura.agents.piflow.agent.domain.RoleResources;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Адаптер запуска Pi через CLI/subprocess на базе {@link ProcessBuilder}.
 *
 * <p>Инкапсулирует создание процесса, передачу роли через системный промпт и
 * скилы, задачу через аргументы командной строки ({@code --print} и первый
 * промпт), параллельное чтение stdout/stderr, timeout, отмену и освобождение
 * ресурсов. Локальные Git-операции и workflow-логику не выполняет.</p>
 */
@Slf4j
@Component
public class ProcessAgentRunner implements AgentProcessRunner {

	private final AgentProperties properties;
	private final RoleResources roleResources;
	private final UnaryOperator<String> envLookup;
	private final Map<String, Process> activeProcesses = new ConcurrentHashMap<>();
	private final Set<String> cancelledRuns = ConcurrentHashMap.newKeySet();
	private final ExecutorService ioExecutor = Executors.newCachedThreadPool(daemonThreadFactory());

	@Autowired
	public ProcessAgentRunner(AgentProperties properties, RoleResources roleResources) {
		this(properties, roleResources, System::getenv);
	}

	ProcessAgentRunner(AgentProperties properties, RoleResources roleResources, UnaryOperator<String> envLookup) {
		this.properties = properties;
		this.roleResources = roleResources;
		this.envLookup = envLookup;
	}

	@Override
	public AgentProcessResult execute(AgentProcessRequest request) {
		validateExecutable();
		Path executablePath = resolveExecutable(properties.executable());
		log.info("pi process starting: runId={} role={} workingDirectory={}",
				request.runId(), request.role(), request.workingDirectory());

		Process process = null;
		try {
			java.util.List<String> command = buildCommand(request, executablePath);
			log.debug("pi command: runId={} command={}", request.runId(), command);
			ProcessBuilder builder = new ProcessBuilder(command);
			builder.directory(request.workingDirectory().toFile());
			Map<String, String> environment = builder.environment();
			environment.clear();
			environment.putAll(resolveEnvironment(executablePath));

			process = builder.start();
			activeProcesses.put(request.runId(), process);

			CompletableFuture<String> stdoutFuture = readAsync(process.getInputStream());
			CompletableFuture<String> stderrFuture = readAsync(process.getErrorStream());
			closeStdin(process);

			boolean finished = waitForProcess(process, effectiveTimeout(request));

			AgentProcessStatus status;
			Integer exitCode = null;
			if (cancelledRuns.remove(request.runId())) {
				status = AgentProcessStatus.CANCELLED;
			} else if (!finished) {
				status = AgentProcessStatus.TIMED_OUT;
				destroyProcess(process);
			} else {
				exitCode = process.exitValue();
				status = exitCode == 0 ? AgentProcessStatus.SUCCEEDED : AgentProcessStatus.FAILED;
			}

			String stdout = stdoutFuture.get(30, TimeUnit.SECONDS);
			String stderr = stderrFuture.get(30, TimeUnit.SECONDS);
			log.info("pi process finished: runId={} status={} exitCode={}",
					request.runId(), status, exitCode);
			log.debug("pi stdout: runId={} output={}", request.runId(), stdout);
			log.debug("pi stderr: runId={} output={}", request.runId(), stderr);
			return new AgentProcessResult(request.runId(), status, stdout, stderr, exitCode);
		} catch (IOException e) {
			throw AgentProcessException.launch("failed to start Pi process: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw AgentProcessException.execution("interrupted while running Pi", e);
		} catch (ExecutionException e) {
			throw AgentProcessException.result("failed to read Pi output", e.getCause());
		} catch (TimeoutException e) {
			throw AgentProcessException.result("timed out reading Pi output", e);
		} finally {
			activeProcesses.remove(request.runId());
			if (process != null && process.isAlive()) {
				process.destroyForcibly();
			}
		}
	}

	@Override
	public void cancel(String runId) {
		cancelledRuns.add(runId);
		Process process = activeProcesses.get(runId);
		if (process != null) {
			destroyProcess(process);
		}
	}

	private void validateExecutable() {
		String executable = properties.executable();
		if (executable == null || executable.isBlank()) {
			throw AgentProcessException.launch("agent executable is not configured", null);
		}
		if (executable.contains("/") || executable.contains("\\")) {
			Path path = Path.of(executable);
			if (!Files.isRegularFile(path) || !Files.isExecutable(path)) {
				throw AgentProcessException.launch("agent executable is missing or not executable: " + executable, null);
			}
		}
	}

	private List<String> buildCommand(AgentProcessRequest request, Path executablePath) {
		List<String> command = new ArrayList<>();
		command.add(executablePath.toString());
		command.addAll(properties.baseArgs());
		command.add("--print");
		command.add("--append-system-prompt");
		command.add(roleResources.systemPromptFile(request.role()).toString());
		command.add("--skill");
		command.add(roleResources.skillsDirectory(request.role()).toString());
		command.add("--");
		command.add(buildMessage(request));
		return command;
	}

	private String buildMessage(AgentProcessRequest request) {
		StringBuilder message = new StringBuilder();
		appendText(message, request.instruction());
		appendText(message, request.inputData());
		appendText(message, request.context());
		appendText(message, request.feedback());
		return message.toString();
	}

	private void appendText(StringBuilder message, String text) {
		if (text == null || text.isBlank()) {
			return;
		}
		if (!message.isEmpty()) {
			message.append("\n\n");
		}
		message.append(text);
	}

	private Path resolveExecutable(String executable) {
		Path path = Path.of(executable);
		if (executable.contains("/") || executable.contains("\\")) {
			return path.toAbsolutePath();
		}
		String pathVariable = System.getenv("PATH");
		if (pathVariable != null) {
			for (String dir : pathVariable.split(File.pathSeparator)) {
				if (dir.isBlank()) {
					continue;
				}
				Path candidate = Path.of(dir, executable);
				if (Files.isExecutable(candidate)) {
					return candidate;
				}
			}
		}
		throw AgentProcessException.launch(
				"agent executable not found in PATH: " + executable + "; set agent.executable to an absolute path", null);
	}

	private Map<String, String> resolveEnvironment(Path executablePath) {
		Map<String, String> environment = new HashMap<>();
		for (String name : properties.allowedEnvironmentVariables()) {
			String value = envLookup.apply(name);
			if (value != null) {
				environment.put(name, value);
			}
		}
		// Каталог исполняемого файла добавляется в PATH, чтобы shebang вида
		// `#!/usr/bin/env node` мог найти интерпретатор рядом с исполняемым файлом.
		if (executablePath != null && executablePath.getParent() != null) {
			String dir = executablePath.getParent().toString();
			String existingPath = environment.get("PATH");
			environment.put("PATH", existingPath == null || existingPath.isBlank()
					? dir : dir + File.pathSeparator + existingPath);
		}
		return environment;
	}

	private void closeStdin(Process process) throws IOException {
		try (var stdin = process.getOutputStream()) {
			// stdin закрывается без payload: задача передаётся аргументами командной строки.
		}
	}

	private CompletableFuture<String> readAsync(InputStream stream) {
		return CompletableFuture.supplyAsync(() -> read(stream), ioExecutor);
	}

	private String read(InputStream stream) {
		StringBuilder result = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			char[] buffer = new char[8192];
			int read;
			while ((read = reader.read(buffer)) != -1) {
				result.append(buffer, 0, read);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return result.toString();
	}

	private Duration effectiveTimeout(AgentProcessRequest request) {
		return request.timeout() != null ? request.timeout() : properties.defaultTimeout();
	}

	private boolean waitForProcess(Process process, Duration timeout) throws InterruptedException {
		if (timeout == null) {
			process.waitFor();
			return true;
		}
		return process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
	}

	private void destroyProcess(Process process) {
		process.destroy();
		try {
			if (!process.waitFor(5, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				process.waitFor(5, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			process.destroyForcibly();
		}
	}

	private static ThreadFactory daemonThreadFactory() {
		return runnable -> {
			Thread thread = new Thread(runnable, "agent-io");
			thread.setDaemon(true);
			return thread;
		};
	}
}
