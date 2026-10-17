package com.hydroyura.agents.piflow.agent.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiRunResultTest {

    @Test
    void construction_shouldExposeValues() {
        PiRunResult success = new PiRunResult(PiRunStatus.SUCCESS, "hello", "");

        assertThat(success.status()).isEqualTo(PiRunStatus.SUCCESS);
        assertThat(success.finalText()).isEqualTo("hello");
        assertThat(success.diagnostics()).isEmpty();
    }

    @Test
    void construction_withFailure_shouldExposeDiagnostics() {
        PiRunResult failure = new PiRunResult(PiRunStatus.FAILURE, "", "error");

        assertThat(failure.status()).isEqualTo(PiRunStatus.FAILURE);
        assertThat(failure.diagnostics()).isEqualTo("error");
    }
}
