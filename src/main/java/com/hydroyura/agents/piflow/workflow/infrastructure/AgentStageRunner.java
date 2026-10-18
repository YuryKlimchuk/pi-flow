package com.hydroyura.agents.piflow.workflow.infrastructure;

import com.hydroyura.agents.piflow.agent.api.PiRunResult;
import com.hydroyura.agents.piflow.agent.api.PiRunStatus;
import com.hydroyura.agents.piflow.agent.api.PiRunner;
import com.hydroyura.agents.piflow.workflow.api.ReviewVerdict;
import com.hydroyura.agents.piflow.workflow.api.StageOutcome;
import com.hydroyura.agents.piflow.workflow.api.WorkflowStage;
import com.hydroyura.agents.piflow.workflow.application.StageContext;
import com.hydroyura.agents.piflow.workflow.application.StageRunner;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Executes agent stages by mapping the stage to a role and invoking Pi under that role. */
@Component
public class AgentStageRunner implements StageRunner {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String JSON_FENCE_OPEN = "```json";
    private static final String JSON_FENCE_CLOSE = "```";

    private static final String REVIEW_VERDICT_INSTRUCTION = """
            As the final part of your response, emit exactly one JSON code fence using the ```json marker,
            containing only a single JSON object of the exact shape
            {"verdict": "APPROVED" | "CHANGES_REQUIRED", "findings": ["..."]}.
            The "verdict" value MUST be exactly "APPROVED" or "CHANGES_REQUIRED" (uppercase, no other value),
            and "findings" MUST be a JSON array of strings (it may be empty). Do not emit any other ```json block.
            """;

    private final PiRunner piRunner;

    public AgentStageRunner(PiRunner piRunner) {
        this.piRunner = Objects.requireNonNull(piRunner, "piRunner must not be null");
    }

    @Override
    public StageOutcome run(WorkflowStage stage, Workflow workflow, StageContext context) {
        String agent = agentFor(stage);
        if (agent == null) {
            return StageOutcome.success(stage, "");
        }
        PiRunResult result = piRunner.run(agent, promptFor(stage, context), workflow.getTargetRepo());

        if (stage == WorkflowStage.REVIEW) {
            if (result.status() != PiRunStatus.SUCCESS) {
                return StageOutcome.failure(stage, result.diagnostics());
            }
            return extractReviewOutcome(result);
        }

        return result.status() == PiRunStatus.SUCCESS
                ? StageOutcome.success(stage, result.finalText())
                : StageOutcome.failure(stage, result.finalText());
    }

    private String agentFor(WorkflowStage stage) {
        return switch (stage) {
            case PROJECT_ANALYSIS -> "analyst";
            case CHANGE_PROPOSAL -> "architect";
            case IMPLEMENTATION -> "developer";
            case REVIEW -> "reviewer";
            default -> null;
        };
    }

    private String promptFor(WorkflowStage stage, StageContext context) {
        return switch (stage) {
            case PROJECT_ANALYSIS -> "Analyze the project at " + context.targetRepo() + " and report findings.";
            case CHANGE_PROPOSAL -> {
                StringBuilder sb = new StringBuilder("Prepare OpenSpec change artifacts for the task: " + context.task());
                context.pendingFeedback().ifPresent(f -> sb.append("\nUser feedback: ").append(f));
                yield sb.toString();
            }
            case IMPLEMENTATION -> {
                StringBuilder sb = new StringBuilder("Implement the approved change for the task: " + context.task());
                context.analysisFindings().ifPresent(f -> sb.append("\nAnalysis findings: ").append(f));
                context.proposalOutput().ifPresent(p -> sb.append("\nApproved proposal: ").append(p));
                context.pendingFeedback().ifPresent(f -> sb.append("\nUser feedback: ").append(f));
                yield sb.toString();
            }
            case REVIEW -> {
                StringBuilder sb = new StringBuilder("Review the implementation for the task: " + context.task());
                context.analysisFindings().ifPresent(f -> sb.append("\nAnalysis findings: ").append(f));
                context.proposalOutput().ifPresent(p -> sb.append("\nApproved proposal: ").append(p));
                sb.append('\n').append(REVIEW_VERDICT_INSTRUCTION);
                yield sb.toString();
            }
            default -> context.task();
        };
    }

    private StageOutcome extractReviewOutcome(PiRunResult result) {
        String text = result.finalText();
        List<String> blocks = fencedJsonBlocks(text);
        if (blocks.size() > 1) {
            return StageOutcome.failure(WorkflowStage.REVIEW, "multiple ```json blocks: verdict extraction is ambiguous");
        }
        String candidate = blocks.size() == 1 ? blocks.get(0) : text.trim();

        JsonNode node;
        try {
            node = MAPPER.readTree(candidate);
        } catch (JacksonException e) {
            return StageOutcome.failure(WorkflowStage.REVIEW, "not valid JSON: " + e.getMessage());
        }
        if (node == null || !node.isObject()) {
            return StageOutcome.failure(WorkflowStage.REVIEW, "not a JSON object");
        }

        JsonNode verdictNode = node.get("verdict");
        if (verdictNode == null || !verdictNode.isTextual()) {
            return StageOutcome.failure(WorkflowStage.REVIEW, "missing or invalid verdict");
        }
        ReviewVerdict verdict;
        String verdictText = verdictNode.asText();
        if ("APPROVED".equals(verdictText)) {
            verdict = ReviewVerdict.APPROVED;
        } else if ("CHANGES_REQUIRED".equals(verdictText)) {
            verdict = ReviewVerdict.CHANGES_REQUIRED;
        } else {
            return StageOutcome.failure(WorkflowStage.REVIEW, "missing or invalid verdict");
        }

        JsonNode findingsNode = node.get("findings");
        if (findingsNode == null || !findingsNode.isArray()) {
            return StageOutcome.failure(WorkflowStage.REVIEW, "missing or invalid findings");
        }
        List<String> findings = new ArrayList<>();
        for (JsonNode element : findingsNode) {
            if (!element.isTextual()) {
                return StageOutcome.failure(WorkflowStage.REVIEW, "missing or invalid findings");
            }
            findings.add(element.asText());
        }

        return StageOutcome.reviewed(WorkflowStage.REVIEW, text, verdict, findings);
    }

    private static List<String> fencedJsonBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        List<String> lines = text.lines().toList();
        int i = 0;
        while (i < lines.size()) {
            if (JSON_FENCE_OPEN.equals(lines.get(i).trim())) {
                StringBuilder content = new StringBuilder();
                int j = i + 1;
                boolean closed = false;
                while (j < lines.size()) {
                    if (JSON_FENCE_CLOSE.equals(lines.get(j).trim())) {
                        closed = true;
                        break;
                    }
                    content.append(lines.get(j)).append('\n');
                    j++;
                }
                if (closed) {
                    blocks.add(content.toString());
                    i = j + 1;
                } else {
                    i++;
                }
            } else {
                i++;
            }
        }
        return blocks;
    }
}
