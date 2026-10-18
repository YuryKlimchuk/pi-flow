package com.hydroyura.agents.piflow.workflow.infrastructure;

import com.hydroyura.agents.piflow.workflow.application.ChangeValidator;
import com.hydroyura.agents.piflow.workflow.application.ValidationResult;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Runs {@code openspec validate <changeId> --strict} as a bounded local process in the target repository.
 */
@Component
public class OpenSpecChangeValidator implements ChangeValidator {

    private final String command;
    private final Duration timeout;

    public OpenSpecChangeValidator() {
        this("openspec", Duration.ofSeconds(60));
    }

    public OpenSpecChangeValidator(String command, Duration timeout) {
        this.command = Objects.requireNonNull(command, "command must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
    }

    @Override
    public ValidationResult validate(String targetRepo, String changeId) {
        Objects.requireNonNull(targetRepo, "targetRepo must not be null");
        Objects.requireNonNull(changeId, "changeId must not be null");

        ExternalCommand.Result result = ExternalCommand.run(
                List.of(command, "validate", changeId, "--strict"), Path.of(targetRepo), timeout);

        if (result.startFailed()) {
            return ValidationResult.failed("failed to start openspec validator: " + result.output());
        }
        if (result.timedOut()) {
            return ValidationResult.failed("openspec validate --strict timed out");
        }
        if (result.exitCode() == 0) {
            return ValidationResult.ok();
        }
        return ValidationResult.failed("openspec validate --strict failed: " + result.output());
    }
}
