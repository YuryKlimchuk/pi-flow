package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hydroyura.agents.piflow.agent.api.PiRunResult;
import com.hydroyura.agents.piflow.agent.api.PiRunStatus;
import com.hydroyura.agents.piflow.agent.api.PiRunner;
import com.hydroyura.agents.piflow.agent.infrastructure.AgentProperties;
import com.hydroyura.agents.piflow.agent.infrastructure.JsonModePiRunner;
import com.hydroyura.agents.piflow.agent.infrastructure.PiProperties;
import com.hydroyura.agents.piflow.agent.infrastructure.ResourceAgentResolver;
import com.hydroyura.agents.piflow.workflow.api.ReviewVerdict;
import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowId;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.application.StageContext;
import com.hydroyura.agents.piflow.workflow.application.StageRunner;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class AgentStageRunnerTest {

    @TempDir
    static Path tempDir;

    @Test
    void run_shouldInvokePiUnderRoleInTargetRepository() throws IOException {
        Path targetRepo = tempDir.resolve("target-repo");
        Files.createDirectories(targetRepo);

        PiRunner piRunner = new JsonModePiRunner(
                new PiProperties(cwdPi().toString(), Duration.ofSeconds(10), null),
                new ResourceAgentResolver(
                        new AgentProperties(Map.of("cheap", "fake-model"), Map.of("analyst", "cheap")),
                        new PathMatchingResourcePatternResolver()));
        StageRunner runner = new AgentStageRunner(piRunner);

        Workflow workflow = new Workflow(WorkflowId.generate(), "task", targetRepo.toString());
        StageOutcome outcome = runner.run(WorkflowStage.PROJECT_ANALYSIS, workflow, emptyContext());

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.output()).isEqualTo("ok");
        assertThat(Files.readString(targetRepo.resolve("cwd.txt")).trim())
                .isEqualTo(targetRepo.toString());
    }

    @Test
    void review_singleJsonBlockApproved_shouldYieldApprovedVerdict() {
        StageOutcome outcome = runReview("""
                Here is my review.
                ```json
                {"verdict":"APPROVED","findings":[]}
                ```
                """);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.verdict()).contains(ReviewVerdict.APPROVED);
        assertThat(outcome.findings()).contains(List.of());
    }

    @Test
    void review_singleJsonBlockChangesRequired_shouldYieldChangesRequiredWithFindings() {
        StageOutcome outcome = runReview("""
                ```json
                {"verdict":"CHANGES_REQUIRED","findings":["x","y"]}
                ```
                """);

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.verdict()).contains(ReviewVerdict.CHANGES_REQUIRED);
        assertThat(outcome.findings()).contains(List.of("x", "y"));
    }

    @Test
    void review_noFencedBlockWholeTextJson_shouldSucceed() {
        StageOutcome outcome = runReview("{\"verdict\":\"APPROVED\",\"findings\":[]}");

        assertThat(outcome.success()).isTrue();
        assertThat(outcome.verdict()).contains(ReviewVerdict.APPROVED);
        assertThat(outcome.findings()).contains(List.of());
    }

    @Test
    void review_failureStatus_shouldReturnFailureWithoutVerdict() {
        StageOutcome outcome = runReview(new PiRunResult(PiRunStatus.FAILURE, "", "boom"));

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.verdict()).isEmpty();
        assertThat(outcome.findings()).isEmpty();
    }

    @Test
    void review_timeoutStatus_shouldReturnFailureWithoutVerdict() {
        StageOutcome outcome = runReview(new PiRunResult(PiRunStatus.TIMEOUT, "", "timed out"));

        assertThat(outcome.success()).isFalse();
        assertThat(outcome.verdict()).isEmpty();
        assertThat(outcome.findings()).isEmpty();
    }

    @Test
    void review_twoJsonBlocks_shouldFailAsAmbiguous() {
        StageOutcome outcome = runReview("""
                ```json
                {"verdict":"APPROVED","findings":[]}
                ```
                ```json
                {"verdict":"APPROVED","findings":[]}
                ```
                """);

        assertExtractionFailure(outcome);
    }

    @Test
    void review_invalidJson_shouldFail() {
        assertExtractionFailure(runReview("```json\nnot json\n```"));
    }

    @Test
    void review_jsonArray_shouldFail() {
        assertExtractionFailure(runReview("```json\n[1,2]\n```"));
    }

    @Test
    void review_missingVerdictKey_shouldFail() {
        assertExtractionFailure(runReview("```json\n{\"findings\":[]}\n```"));
    }

    @Test
    void review_wrongVerdictString_shouldFail() {
        assertExtractionFailure(runReview("```json\n{\"verdict\":\"approved\",\"findings\":[]}\n```"));
        assertExtractionFailure(runReview("```json\n{\"verdict\":\"MAYBE\",\"findings\":[]}\n```"));
    }

    @Test
    void review_nonStringVerdict_shouldFail() {
        assertExtractionFailure(runReview("```json\n{\"verdict\":42,\"findings\":[]}\n```"));
    }

    @Test
    void review_missingFindingsKey_shouldFail() {
        assertExtractionFailure(runReview("```json\n{\"verdict\":\"APPROVED\"}\n```"));
    }

    @Test
    void review_nonArrayFindings_shouldFail() {
        assertExtractionFailure(runReview("```json\n{\"verdict\":\"APPROVED\",\"findings\":\"nope\"}\n```"));
    }

    @Test
    void review_findingsWithNonStringElement_shouldFail() {
        assertExtractionFailure(runReview("```json\n{\"verdict\":\"APPROVED\",\"findings\":[1]}\n```"));
    }

    @Test
    void reviewPrompt_shouldInstructJsonVerdictShape() {
        StubPiRunner stub = new StubPiRunner(new PiRunResult(PiRunStatus.SUCCESS, "{\"verdict\":\"APPROVED\",\"findings\":[]}", ""));
        StageRunner runner = new AgentStageRunner(stub);
        runner.run(WorkflowStage.REVIEW, workflow(), emptyContext());

        assertThat(stub.lastPrompt())
                .contains("```json")
                .contains("\"verdict\"")
                .contains("\"findings\"")
                .contains("APPROVED")
                .contains("CHANGES_REQUIRED");
    }

    @Test
    void implementationPrompt_shouldIncludeFindingsProposalAndFeedbackWhenPresent() {
        StubPiRunner stub = new StubPiRunner(new PiRunResult(PiRunStatus.SUCCESS, "ok", ""));
        StageRunner runner = new AgentStageRunner(stub);
        StageContext context = new StageContext("task", "/repo",
                Optional.of("FINDINGS"), Optional.of("PROPOSAL"), Optional.of("FEEDBACK"));

        runner.run(WorkflowStage.IMPLEMENTATION, workflow(), context);

        assertThat(stub.lastPrompt()).contains("FINDINGS").contains("PROPOSAL").contains("FEEDBACK");
    }

    @Test
    void reviewPrompt_shouldIncludeFindingsAndProposalWhenPresent() {
        StubPiRunner stub = new StubPiRunner(new PiRunResult(PiRunStatus.SUCCESS, "{\"verdict\":\"APPROVED\",\"findings\":[]}", ""));
        StageRunner runner = new AgentStageRunner(stub);
        StageContext context = new StageContext("task", "/repo",
                Optional.of("FINDINGS"), Optional.of("PROPOSAL"), Optional.empty());

        runner.run(WorkflowStage.REVIEW, workflow(), context);

        assertThat(stub.lastPrompt()).contains("FINDINGS").contains("PROPOSAL");
    }

    @Test
    void proposalPrompt_shouldIncludeFeedbackWhenPresent() {
        StubPiRunner stub = new StubPiRunner(new PiRunResult(PiRunStatus.SUCCESS, "ok", ""));
        StageRunner runner = new AgentStageRunner(stub);
        StageContext context = new StageContext("task", "/repo",
                Optional.empty(), Optional.empty(), Optional.of("FEEDBACK"));

        runner.run(WorkflowStage.CHANGE_PROPOSAL, workflow(), context);

        assertThat(stub.lastPrompt()).contains("FEEDBACK");
    }

    @Test
    void prompt_shouldOmitFeedbackFindingsProposalWhenAbsent() {
        StubPiRunner stub = new StubPiRunner(new PiRunResult(PiRunStatus.SUCCESS, "ok", ""));
        StageRunner runner = new AgentStageRunner(stub);

        runner.run(WorkflowStage.IMPLEMENTATION, workflow(), emptyContext());

        assertThat(stub.lastPrompt())
                .doesNotContain("Analysis findings")
                .doesNotContain("Approved proposal")
                .doesNotContain("User feedback");
    }

    private StageOutcome runReview(String finalText) {
        return runReview(new PiRunResult(PiRunStatus.SUCCESS, finalText, ""));
    }

    private StageOutcome runReview(PiRunResult result) {
        StubPiRunner stub = new StubPiRunner(result);
        StageRunner runner = new AgentStageRunner(stub);
        return runner.run(WorkflowStage.REVIEW, workflow(), emptyContext());
    }

    private static Workflow workflow() {
        return new Workflow(WorkflowId.generate(), "task", "/repo");
    }

    private static StageContext emptyContext() {
        return new StageContext("task", "/repo", Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static void assertExtractionFailure(StageOutcome outcome) {
        assertThat(outcome.success()).isFalse();
        assertThat(outcome.verdict()).isEmpty();
        assertThat(outcome.findings()).isEmpty();
    }

    private static Path cwdPi() throws IOException {
        Path cwdPi = tempDir.resolve("fake-pi-cwd");
        Files.writeString(cwdPi, """
                #!/bin/sh
                printf '%s\\n' "$PWD" > cwd.txt
                printf '%s\\n' '{"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"ok"}]}}'
                printf '%s\\n' '{"type":"agent_settled","aborted":false}'
                """);
        if (!cwdPi.toFile().setExecutable(true)) {
            throw new IllegalStateException("could not make fake-pi-cwd executable");
        }
        return cwdPi;
    }

    private static final class StubPiRunner implements PiRunner {

        private final PiRunResult result;
        private String lastPrompt;

        StubPiRunner(PiRunResult result) {
            this.result = result;
        }

        @Override
        public PiRunResult run(String prompt) {
            this.lastPrompt = prompt;
            return result;
        }

        @Override
        public PiRunResult run(String agentName, String prompt, String workingDirectory) {
            this.lastPrompt = prompt;
            return result;
        }

        String lastPrompt() {
            return lastPrompt;
        }
    }
}
