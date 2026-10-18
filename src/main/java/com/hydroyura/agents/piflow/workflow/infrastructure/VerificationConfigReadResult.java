package com.hydroyura.agents.piflow.workflow.infrastructure;

import java.nio.file.Path;
import java.util.Objects;

/** Outcome of reading a target repository's verification configuration file. */
public sealed interface VerificationConfigReadResult {

    /** The configuration file does not exist. */
    record Absent(Path resolvedPath) implements VerificationConfigReadResult {
        public Absent {
            resolvedPath = Objects.requireNonNull(resolvedPath, "resolvedPath must not be null");
        }
    }

    /** The configuration file exists but cannot be read or does not satisfy the fixed schema. */
    record Invalid(String reason) implements VerificationConfigReadResult {
        public Invalid {
            reason = Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    /** The configuration file exists and parses as a valid configuration. */
    record Valid(VerificationConfig config) implements VerificationConfigReadResult {
        public Valid {
            Objects.requireNonNull(config, "config must not be null");
        }
    }
}
