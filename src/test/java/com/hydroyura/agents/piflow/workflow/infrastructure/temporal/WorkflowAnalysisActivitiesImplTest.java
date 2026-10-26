package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.hydroyura.agents.piflow.agent.api.AgentException;
import com.hydroyura.agents.piflow.agent.api.AgentOperations;
import com.hydroyura.agents.piflow.agent.api.AgentRole;
import com.hydroyura.agents.piflow.agent.api.AgentRunResult;
import com.hydroyura.agents.piflow.agent.api.AgentRunStatus;
import com.hydroyura.agents.piflow.agent.api.StartAgentCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit-тест activity анализа: взаимодействие с публичным {@link AgentOperations}
 * и валидация результата/артефакта.
 */
@ExtendWith(MockitoExtension.class)
class WorkflowAnalysisActivitiesImplTest {

	private static final String ARTIFACT = ".pi-flow/analysis/analyst.md";
	private static final String BRANCH = ".pi-flow/analysis/branch.txt";

	@TempDir
	Path workspace;

	@Mock
	private AgentOperations agentOperations;

	private WorkflowAnalysisActivitiesImpl activity;

	@BeforeEach
	void setUp() {
		activity = new WorkflowAnalysisActivitiesImpl(agentOperations);
	}

	@Test
	void analyze_whenSuccessful_shouldReturnArtifactMetadata() throws IOException {
		writeArtifact("## Понимание задачи\nСодержимое анализа");
		writeBranch("feature/issue-42");
		when(agentOperations.run(any())).thenReturn(successResult());

		WorkflowAnalysisResult result = activity.analyze(input());

		assertThat(result.workspacePath()).isEqualTo(workspace.toString());
		assertThat(result.branchName()).isEqualTo("feature/issue-42");
		assertThat(result.runId()).isEqualTo("run-1");
		assertThat(result.artifactSize()).isGreaterThan(0);

		ArgumentCaptor<StartAgentCommand> captor = ArgumentCaptor.forClass(StartAgentCommand.class);
		verify(agentOperations).run(captor.capture());
		StartAgentCommand command = captor.getValue();
		assertThat(command.role()).isEqualTo(AgentRole.ANALYST);
		assertThat(command.workingDirectory()).isEqualTo(workspace);
		assertThat(command.instruction()).contains("acme/demo#42");
		assertThat(command.instruction()).contains(workspace.resolve(ARTIFACT).toString());
	}

	@Test
	void analyze_whenAgentFailed_shouldThrowAgentFailed() {
		when(agentOperations.run(any())).thenReturn(
				new AgentRunResult("run-1", AgentRole.ANALYST, AgentRunStatus.FAILED, "", "err", 1, Map.of()));

		WorkflowAnalysisException error = catchThrowableOfType(() -> activity.analyze(input()),
				WorkflowAnalysisException.class);

		assertThat(error.reason()).isEqualTo(WorkflowAnalysisException.Reason.AGENT_FAILED);
	}

	@Test
	void analyze_whenTimedOut_shouldThrowAgentFailed() {
		when(agentOperations.run(any())).thenReturn(
				new AgentRunResult("run-1", AgentRole.ANALYST, AgentRunStatus.TIMED_OUT, "", "", null, Map.of()));

		WorkflowAnalysisException error = catchThrowableOfType(() -> activity.analyze(input()),
				WorkflowAnalysisException.class);

		assertThat(error.reason()).isEqualTo(WorkflowAnalysisException.Reason.AGENT_FAILED);
	}

	@Test
	void analyze_whenNonZeroExitCode_shouldThrowAgentFailed() {
		when(agentOperations.run(any())).thenReturn(
				new AgentRunResult("run-1", AgentRole.ANALYST, AgentRunStatus.SUCCEEDED, "out", "", 2, Map.of()));

		WorkflowAnalysisException error = catchThrowableOfType(() -> activity.analyze(input()),
				WorkflowAnalysisException.class);

		assertThat(error.reason()).isEqualTo(WorkflowAnalysisException.Reason.AGENT_FAILED);
	}

	@Test
	void analyze_whenLaunchError_shouldThrowAgentFailed() {
		when(agentOperations.run(any())).thenThrow(AgentException.launch("boom", new RuntimeException()));

		WorkflowAnalysisException error = catchThrowableOfType(() -> activity.analyze(input()),
				WorkflowAnalysisException.class);

		assertThat(error.reason()).isEqualTo(WorkflowAnalysisException.Reason.AGENT_FAILED);
	}

	@Test
	void analyze_whenArtifactMissing_shouldThrowInvalidResult() {
		when(agentOperations.run(any())).thenReturn(successResult());

		WorkflowAnalysisException error = catchThrowableOfType(() -> activity.analyze(input()),
				WorkflowAnalysisException.class);

		assertThat(error.reason()).isEqualTo(WorkflowAnalysisException.Reason.INVALID_RESULT);
	}

	@Test
	void analyze_whenArtifactEmpty_shouldThrowInvalidResult() throws IOException {
		writeArtifact("   \n");
		when(agentOperations.run(any())).thenReturn(successResult());

		WorkflowAnalysisException error = catchThrowableOfType(() -> activity.analyze(input()),
				WorkflowAnalysisException.class);

		assertThat(error.reason()).isEqualTo(WorkflowAnalysisException.Reason.INVALID_RESULT);
	}

	@Test
	void analyze_whenMissingWorkspace_shouldThrowInvalidResult() {
		WorkflowAnalysisException error = catchThrowableOfType(
				() -> activity.analyze(new WorkflowAnalysisInput("wf-1", "url", "acme", "demo", 42, null, null, null)),
				WorkflowAnalysisException.class);

		assertThat(error.reason()).isEqualTo(WorkflowAnalysisException.Reason.INVALID_RESULT);
	}

	@Test
	void analyze_whenRepeated_shouldKeepForeignFiles() throws IOException {
		writeArtifact("анализ");
		writeBranch("feature/issue-42");
		Path foreign = workspace.resolve("foreign.txt");
		Files.writeString(foreign, "чужие данные");
		when(agentOperations.run(any())).thenReturn(successResult());

		WorkflowAnalysisResult result = activity.analyze(input());

		assertThat(result.workspacePath()).isNotBlank();
		assertThat(Files.readString(foreign)).isEqualTo("чужие данные");
	}

	@Test
	void analyze_whenBranchMissing_shouldThrowInvalidResult() throws IOException {
		writeArtifact("анализ");
		when(agentOperations.run(any())).thenReturn(successResult());

		WorkflowAnalysisException error = catchThrowableOfType(() -> activity.analyze(input()),
				WorkflowAnalysisException.class);

		assertThat(error.reason()).isEqualTo(WorkflowAnalysisException.Reason.INVALID_RESULT);
	}

	@Test
	void analyze_whenBranchInvalid_shouldThrowInvalidResult() throws IOException {
		writeArtifact("анализ");
		writeBranch("ветка с пробелом");
		when(agentOperations.run(any())).thenReturn(successResult());

		WorkflowAnalysisException error = catchThrowableOfType(() -> activity.analyze(input()),
				WorkflowAnalysisException.class);

		assertThat(error.reason()).isEqualTo(WorkflowAnalysisException.Reason.INVALID_RESULT);
	}

	@Test
	void isValidBranchName_shouldAcceptValidNames() {
		assertThat(WorkflowAnalysisActivitiesImpl.isValidBranchName("feature/issue-42-login")).isTrue();
		assertThat(WorkflowAnalysisActivitiesImpl.isValidBranchName("main")).isTrue();
		assertThat(WorkflowAnalysisActivitiesImpl.isValidBranchName("fix_42")).isTrue();
	}

	@Test
	void isValidBranchName_shouldRejectInvalidNames() {
		assertThat(WorkflowAnalysisActivitiesImpl.isValidBranchName(null)).isFalse();
		assertThat(WorkflowAnalysisActivitiesImpl.isValidBranchName("")).isFalse();
		assertThat(WorkflowAnalysisActivitiesImpl.isValidBranchName("feature branch")).isFalse();
		assertThat(WorkflowAnalysisActivitiesImpl.isValidBranchName("ветка")).isFalse();
		assertThat(WorkflowAnalysisActivitiesImpl.isValidBranchName("feature..x")).isFalse();
		assertThat(WorkflowAnalysisActivitiesImpl.isValidBranchName("/feature")).isFalse();
		assertThat(WorkflowAnalysisActivitiesImpl.isValidBranchName("feature/")).isFalse();
		assertThat(WorkflowAnalysisActivitiesImpl.isValidBranchName("feature.")).isFalse();
		assertThat(WorkflowAnalysisActivitiesImpl.isValidBranchName("feature@{x}")).isFalse();
	}

	@Test
	void analyze_whenFeedbackPresent_shouldIncludeInInstruction() throws IOException {
		writeArtifact("анализ");
		writeBranch("feature/issue-42");
		when(agentOperations.run(any())).thenReturn(successResult());

		activity.analyze(new WorkflowAnalysisInput("wf-1", "https://github.com/acme/demo.git", "acme", "demo", 42,
				workspace.toString(), "Добавь риски миграции", null));

		ArgumentCaptor<StartAgentCommand> captor = ArgumentCaptor.forClass(StartAgentCommand.class);
		verify(agentOperations).run(captor.capture());
		assertThat(captor.getValue().instruction()).contains("Добавь риски миграции");
	}

	private WorkflowAnalysisInput input() {
		return new WorkflowAnalysisInput("wf-1", "https://github.com/acme/demo.git", "acme", "demo", 42,
				workspace.toString(), null, null);
	}

	private AgentRunResult successResult() {
		return new AgentRunResult("run-1", AgentRole.ANALYST, AgentRunStatus.SUCCEEDED, "out", "", 0, Map.of());
	}

	private void writeArtifact(String content) throws IOException {
		Path artifact = workspace.resolve(ARTIFACT);
		Files.createDirectories(artifact.getParent());
		Files.writeString(artifact, content);
	}

	private void writeBranch(String branchName) throws IOException {
		Files.writeString(workspace.resolve(BRANCH), branchName);
	}
}
