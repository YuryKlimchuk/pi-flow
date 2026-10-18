package com.hydroyura.agents.piflow.workflow.infrastructure;

import com.hydroyura.agents.piflow.workflow.application.VerificationResult;
import com.hydroyura.agents.piflow.workflow.application.Verifier;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Runs the target repository's configured verification command and reports a four-valued result.
 *
 * <p>The configuration file is re-read fresh on every {@link #verify(Workflow)} call via
 * {@link VerificationConfigReader}, so configuration changes take effect on the next attempt
 * without an application restart. Captured output is bounded at {@link #MAX_REPORT_CHARS} characters;
 * once the bound is hit, further output is drained but discarded and the result is flagged truncated.
 */
@Component
@Slf4j
public class CommandVerifier implements Verifier {

    static final int MAX_REPORT_CHARS = 20000;

    private final VerificationConfigReader configReader;

    public CommandVerifier(VerificationProperties properties) {
        Objects.requireNonNull(properties, "properties must not be null");
        this.configReader = new VerificationConfigReader(properties);
    }

    @Override
    public VerificationResult verify(Workflow workflow) {
        Path targetRepo = Path.of(workflow.getTargetRepo());
        return switch (configReader.read(targetRepo)) {
            case VerificationConfigReadResult.Absent absent ->
                    VerificationResult.notConfigured("No verification configuration file found at " + absent.resolvedPath());
            case VerificationConfigReadResult.Invalid invalid ->
                    VerificationResult.infrastructureError(invalid.reason());
            case VerificationConfigReadResult.Valid valid -> runCommand(valid.config(), targetRepo);
        };
    }

    private VerificationResult runCommand(VerificationConfig config, Path targetRepo) {
        ProcessBuilder processBuilder = new ProcessBuilder(config.command());
        Path workingDirectory = config.workingDirectory().map(Path::of).orElse(targetRepo);
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(true);

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            log.warn("Failed to start verification command {} in {}", config.command(), workingDirectory, e);
            return VerificationResult.infrastructureError(
                    "Failed to start verification command " + config.command() + " in " + workingDirectory + ": " + e.getMessage());
        }
        closeStdin(process);

        BoundedOutput output = new BoundedOutput(MAX_REPORT_CHARS);
        Thread readerThread = startOutputReader(process, output);

        boolean finished;
        try {
            finished = process.waitFor(config.timeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            destroyTree(process);
            joinQuietly(readerThread);
            return VerificationResult.infrastructureError("Interrupted while waiting for verification command");
        }

        if (!finished) {
            destroyTree(process);
            joinQuietly(readerThread);
            log.warn("Verification command {} timed out after {} seconds", config.command(), config.timeoutSeconds());
            return VerificationResult.infrastructureError(
                    "Verification command timed out after " + config.timeoutSeconds() + " seconds");
        }

        joinQuietly(readerThread);
        int exitCode = process.exitValue();
        String report = buildReport(exitCode, output);
        log.info("Verification command finished: exitCode={}, truncated={}", exitCode, output.truncated());
        return exitCode == 0
                ? VerificationResult.passed(report, output.truncated())
                : VerificationResult.failed(report, output.truncated());
    }

    private Thread startOutputReader(Process process, BoundedOutput output) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    output.append(new String(buffer, 0, read));
                }
            } catch (IOException ignored) {
                // stream closed, e.g. after destroy
            }
        }, "verification-output");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static String buildReport(int exitCode, BoundedOutput output) {
        String marker = output.truncated() ? " [output truncated]" : "";
        return "exit code " + exitCode + marker + "\n" + output.report();
    }

    private static void closeStdin(Process process) {
        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {
            // best effort: the process does not read stdin
        }
    }

    private void destroyTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private void joinQuietly(Thread thread) {
        try {
            thread.join(Duration.ofSeconds(5).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Fixed-capacity output buffer; once the cap is hit, further appends are discarded and truncated flips true. */
    private static final class BoundedOutput {

        private final int maxChars;
        private final StringBuilder buffer = new StringBuilder();
        private boolean truncated;

        BoundedOutput(int maxChars) {
            this.maxChars = maxChars;
        }

        void append(String text) {
            if (truncated) {
                return;
            }
            int remaining = maxChars - buffer.length();
            if (text.length() <= remaining) {
                buffer.append(text);
            } else {
                buffer.append(text, 0, remaining);
                truncated = true;
            }
        }

        String report() {
            return buffer.toString();
        }

        boolean truncated() {
            return truncated;
        }
    }
}
