package com.hydroyura.agents.piflow.workflow.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.agents.piflow.agent.api.PiRunResult;
import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

class DiagnosticEntryTest {

    @Test
    void factory_shouldNotAcceptProhibitedTypesOrNames() {
        Method factory = null;
        for (Method method : DiagnosticEntry.class.getDeclaredMethods()) {
            if (method.getName().equals("of") && Modifier.isStatic(method.getModifiers())) {
                factory = method;
                break;
            }
        }
        assertThat(factory).as("DiagnosticEntry.of factory must exist").isNotNull();

        for (Parameter param : factory.getParameters()) {
            assertThat(param.getType())
                    .as("factory must not accept a StageOutcome/PiRunResult/Workflow")
                    .isNotEqualTo(StageOutcome.class)
                    .isNotEqualTo(PiRunResult.class)
                    .isNotEqualTo(Workflow.class);
            assertThat(param.getType().getAnnotation(ConfigurationProperties.class))
                    .as("factory must not accept a @ConfigurationProperties type")
                    .isNull();
        }

        assertThat(factory.getParameters())
                .extracting(Parameter::getName)
                .noneMatch(name -> name.equals("prompt")
                        || name.equals("finalText")
                        || name.equals("output")
                        || name.equals("task"));
    }

    @Test
    void of_shouldBuildReasonFromAllowedFields() {
        DiagnosticEntry entry = DiagnosticEntry.of(WorkflowStage.PROJECT_ANALYSIS, 1,
                Instant.parse("2026-10-18T12:00:00Z"), Duration.ofMillis(10),
                DiagnosticCategory.SUCCESS, null, null, 500);

        assertThat(entry.stage()).isEqualTo(WorkflowStage.PROJECT_ANALYSIS);
        assertThat(entry.attemptSequence()).isEqualTo(1);
        assertThat(entry.category()).isEqualTo(DiagnosticCategory.SUCCESS);
        assertThat(entry.reason()).isEqualTo("SUCCESS");
        assertThat(entry.reasonTruncated()).isFalse();
    }
}
