package com.hydroyura.agents.piflow.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class StageContextTest {

    @Test
    void construct_withOptionalFieldsPresent_shouldExposeValues() {
        StageContext context = new StageContext(
                "task",
                "/repo",
                Optional.of("analysis findings"),
                Optional.of("proposal output"),
                Optional.of("feedback"));

        assertThat(context.task()).isEqualTo("task");
        assertThat(context.targetRepo()).isEqualTo("/repo");
        assertThat(context.analysisFindings()).contains("analysis findings");
        assertThat(context.proposalOutput()).contains("proposal output");
        assertThat(context.pendingFeedback()).contains("feedback");
    }

    @Test
    void construct_withEmptyOptionals_shouldExposeEmpty() {
        StageContext context = new StageContext(
                "task",
                "/repo",
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        assertThat(context.analysisFindings()).isEmpty();
        assertThat(context.proposalOutput()).isEmpty();
        assertThat(context.pendingFeedback()).isEmpty();
    }
}
