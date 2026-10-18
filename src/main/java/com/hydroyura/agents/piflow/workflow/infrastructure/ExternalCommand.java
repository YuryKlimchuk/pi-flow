package com.hydroyura.agents.piflow.workflow.infrastructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs a bounded child process, capturing combined stdout/stderr and distinguishing
 * start failures from timeouts from completed runs.
 */
final class ExternalCommand {

    private ExternalCommand() {
    }

    record Result(int exitCode, String output, boolean timedOut, boolean startFailed) {

        static Result started(int exitCode, String output) {
            return new Result(exitCode, output, false, false);
        }

        static Result timedOut(String output) {
            return new Result(-1, output, true, false);
        }

        static Result startFailure(String reason) {
            return new Result(-1, reason, false, true);
        }
    }

    static Result run(List<String> command, Path workingDirectory, Duration timeout) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(true);

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            return Result.startFailure("Failed to start command " + command + ": " + e.getMessage());
        }
        closeStdin(process);

        StringBuilder output = new StringBuilder();
        Thread reader = startOutputReader(process, output);

        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            destroyTree(process);
            joinQuietly(reader);
            return Result.startFailure("Interrupted while waiting for command " + command);
        }

        if (!finished) {
            destroyTree(process);
            joinQuietly(reader);
            return Result.timedOut(output.toString());
        }

        joinQuietly(reader);
        return Result.started(process.exitValue(), output.toString());
    }

    private static Thread startOutputReader(Process process, StringBuilder output) {
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
        }, "external-command-output");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void closeStdin(Process process) {
        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {
            // best effort: the child does not read stdin
        }
    }

    private static void destroyTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private static void joinQuietly(Thread thread) {
        try {
            thread.join(Duration.ofSeconds(5).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
