package com.hydroyura.agents.piflow.agent.infrastructure;

import com.hydroyura.agents.piflow.agent.api.PiRunResult;
import com.hydroyura.agents.piflow.agent.api.PiRunStatus;
import com.hydroyura.agents.piflow.agent.api.PiRunner;
import com.hydroyura.agents.piflow.agent.application.AgentResolver;
import com.hydroyura.agents.piflow.agent.domain.AgentDefinition;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Runs Pi as a child process in JSON mode and assembles its JSONL output.
 */
@Service
@Slf4j
public class JsonModePiRunner implements PiRunner {

    private final PiProperties properties;
    private final AgentResolver agentResolver;

    public JsonModePiRunner(PiProperties properties, AgentResolver agentResolver) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.agentResolver = Objects.requireNonNull(agentResolver, "agentResolver must not be null");
    }

    @Override
    public PiRunResult run(String prompt) {
        Objects.requireNonNull(prompt, "prompt must not be null");
        return runCommand(List.of(properties.command(), "--mode", "json", "--no-session", prompt));
    }

    @Override
    public PiRunResult run(String agentName, String prompt) {
        Objects.requireNonNull(agentName, "agentName must not be null");
        Objects.requireNonNull(prompt, "prompt must not be null");

        Optional<AgentDefinition> resolved = agentResolver.resolve(agentName);
        if (resolved.isEmpty()) {
            log.warn("Unknown agent: {}", agentName);
            return new PiRunResult(PiRunStatus.FAILURE, "", "Unknown agent: " + agentName);
        }

        AgentDefinition agent = resolved.get();
        Path tempPrompt = null;
        try {
            List<String> command = new ArrayList<>();
            command.add(properties.command());
            command.add("--mode");
            command.add("json");
            command.add("--no-session");
            if (agent.model() != null && !agent.model().isBlank()) {
                command.add("--model");
                command.add(agent.model());
            }
            if (!agent.tools().isEmpty()) {
                command.add("--tools");
                command.add(String.join(",", agent.tools()));
            }
            if (agent.systemPrompt() != null && !agent.systemPrompt().isBlank()) {
                tempPrompt = Files.createTempFile("pi-flow-agent-", ".md");
                Files.writeString(tempPrompt, agent.systemPrompt());
                command.add("--append-system-prompt");
                command.add(tempPrompt.toString());
            }
            command.add(prompt);
            return runCommand(command);
        } catch (IOException e) {
            log.warn("Failed to prepare agent system prompt", e);
            return new PiRunResult(PiRunStatus.FAILURE, "", "Failed to prepare agent system prompt: " + e.getMessage());
        } finally {
            if (tempPrompt != null) {
                try {
                    Files.deleteIfExists(tempPrompt);
                } catch (IOException ignored) {
                    // best effort cleanup
                }
            }
        }
    }

    private PiRunResult runCommand(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (properties.workingDirectory() != null && !properties.workingDirectory().isBlank()) {
            processBuilder.directory(Path.of(properties.workingDirectory()).toFile());
        }
        log.info("Starting Pi run: command={}, cwd={}, timeout={}",
                command, processBuilder.directory(), properties.timeout());

        Process process;
        try {
            process = processBuilder.start();
            // Close the child's stdin (EOF) so Pi does not block reading piped stdin.
            process.getOutputStream().close();
        } catch (IOException e) {
            log.warn("Failed to start Pi process", e);
            return new PiRunResult(PiRunStatus.FAILURE, "", "Failed to start Pi: " + e.getMessage());
        }

        PiJsonlParser parser = new PiJsonlParser();
        StringBuilder stderr = new StringBuilder();

        Thread stdoutThread = startStdoutReader(process, parser);
        Thread stderrThread = startStderrReader(process, stderr);

        boolean finished;
        try {
            finished = process.waitFor(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            destroyTree(process);
            joinQuietly(stdoutThread, stderrThread);
            log.warn("Interrupted while waiting for Pi");
            return new PiRunResult(PiRunStatus.FAILURE, parser.finalText(), "Interrupted while waiting for Pi");
        }

        if (!finished) {
            destroyTree(process);
            joinQuietly(stdoutThread, stderrThread);
            log.warn("Pi run timed out after {}", properties.timeout());
            return new PiRunResult(PiRunStatus.TIMEOUT, parser.finalText(), stderr.toString().trim());
        }

        joinQuietly(stdoutThread, stderrThread);
        int exitCode = process.exitValue();
        PiRunResult result = toResult(parser, exitCode, stderr);
        log.info("Pi run finished: exitCode={}, status={}, finalText={}, diagnostics={}",
                exitCode, result.status(), truncate(result.finalText()), result.diagnostics());
        return result;
    }

    private Thread startStdoutReader(Process process, PiJsonlParser parser) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    parser.accept(line);
                }
            } catch (IOException ignored) {
                // stream closed, e.g. after destroy
            }
        }, "pi-stdout");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private Thread startStderrReader(Process process, StringBuilder stderr) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.append(line).append('\n');
                }
            } catch (IOException ignored) {
                // stream closed, e.g. after destroy
            }
        }, "pi-stderr");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private PiRunResult toResult(PiJsonlParser parser, int exitCode, StringBuilder stderr) {
        PiRunStatus status = (exitCode != 0 || parser.failed()) ? PiRunStatus.FAILURE : PiRunStatus.SUCCESS;
        String diagnostics = stderr.toString().trim();
        if (diagnostics.isEmpty() && parser.failureReason() != null) {
            diagnostics = parser.failureReason();
        }
        return new PiRunResult(status, parser.finalText(), diagnostics);
    }

    private void destroyTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private void joinQuietly(Thread... threads) {
        for (Thread thread : threads) {
            try {
                thread.join(Duration.ofSeconds(5).toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String truncate(String s) {
        if (s == null || s.length() <= 200) {
            return s;
        }
        return s.substring(0, 200) + "...";
    }
}
