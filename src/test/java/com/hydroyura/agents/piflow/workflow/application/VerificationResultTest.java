package com.hydroyura.agents.piflow.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VerificationResultTest {

    @Test
    void passed_shouldProducePassedStatusWithSuppliedReportAndTruncatedFlag() {
        VerificationResult result = VerificationResult.passed("all good", true);

        assertThat(result.status()).isEqualTo(VerificationStatus.PASSED);
        assertThat(result.report()).isEqualTo("all good");
        assertThat(result.truncated()).isTrue();
    }

    @Test
    void failed_shouldProduceFailedStatusWithSuppliedReportAndTruncatedFlag() {
        VerificationResult result = VerificationResult.failed("exit code 1", false);

        assertThat(result.status()).isEqualTo(VerificationStatus.FAILED);
        assertThat(result.report()).isEqualTo("exit code 1");
        assertThat(result.truncated()).isFalse();
    }

    @Test
    void notConfigured_shouldUseReasonAsReportAndNeverTruncate() {
        VerificationResult result = VerificationResult.notConfigured("no file");

        assertThat(result.status()).isEqualTo(VerificationStatus.NOT_CONFIGURED);
        assertThat(result.report()).isEqualTo("no file");
        assertThat(result.truncated()).isFalse();
    }

    @Test
    void infrastructureError_shouldUseReasonAsReportAndNeverTruncate() {
        VerificationResult result = VerificationResult.infrastructureError("missing executable");

        assertThat(result.status()).isEqualTo(VerificationStatus.INFRASTRUCTURE_ERROR);
        assertThat(result.report()).isEqualTo("missing executable");
        assertThat(result.truncated()).isFalse();
    }
}
