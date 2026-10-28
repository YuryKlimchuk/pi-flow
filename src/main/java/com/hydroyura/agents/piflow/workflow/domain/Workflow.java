package com.hydroyura.agents.piflow.workflow.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Агрегат Workflow — владелец состояния бизнес-процесса разработки.
 *
 * <p>Инкапсулирует статус, текущий этап, историю завершённых этапов и контрольные
 * точки. Допустимые переходы задаются таблицей переходов; запрещённый переход
 * отклоняется без изменения состояния.</p>
 */
public final class Workflow {

	/** Максимальное число повторных итераций (rework) одного цикла. */
	public static final int MAX_REWORK = 3;

	private final WorkflowId id;
	private final WorkflowContext context;
	private WorkflowStatus status;
	private WorkflowStage currentStage;
	private final List<WorkflowStage> completedStages = new ArrayList<>();
	private final List<Checkpoint> checkpoints = new ArrayList<>();
	private final List<CompletedWorkflowStep> timeline = new ArrayList<>();
	private String executionId;
	private String branchName;
	private String analysisText;
	private String designText;
	private String implementationRunId;
	private String implementationDiagnostics;
	private String reviewRunId;
	private String reviewText;
	private int implementationReworkCount;
	private int reviewReworkCount;
	private String commitId;
	private String branchUrl;
	private long prNumber;
	private String prUrl;
	private String failureReason;

	private static final Map<WorkflowStage, Set<WorkflowStage>> NEXT_STAGES = nextStages();

	private Workflow(WorkflowId id, WorkflowContext context, WorkflowStatus status, WorkflowStage stage) {
		this.id = id;
		this.context = context;
		this.status = status;
		this.currentStage = stage;
	}

	/**
	 * Создаёт новый Workflow в состоянии {@code CREATED} на этапе
	 * {@link WorkflowStage#ANALYZING_REQUIREMENTS}.
	 */
	public static Workflow create(WorkflowId id, WorkflowContext context) {
		return new Workflow(id, context, WorkflowStatus.CREATED, WorkflowStage.ANALYZING_REQUIREMENTS);
	}

	public WorkflowId id() {
		return id;
	}

	public WorkflowContext context() {
		return context;
	}

	public WorkflowStatus status() {
		return status;
	}

	public WorkflowStage currentStage() {
		return currentStage;
	}

	public List<WorkflowStage> completedStages() {
		return Collections.unmodifiableList(completedStages);
	}

	public List<Checkpoint> checkpoints() {
		return Collections.unmodifiableList(checkpoints);
	}

	public List<CompletedWorkflowStep> timeline() {
		return Collections.unmodifiableList(timeline);
	}

	public void appendStep(String stage, String role, String action, String result, String runId) {
		timeline.add(new CompletedWorkflowStep(stage, role, action, result, runId));
	}

	public String executionId() {
		return executionId;
	}

	public String branchName() {
		return branchName;
	}

	public String analysisText() {
		return analysisText;
	}

	public String designText() {
		return designText;
	}

	public String implementationRunId() {
		return implementationRunId;
	}

	public String implementationDiagnostics() {
		return implementationDiagnostics;
	}

	public String reviewRunId() {
		return reviewRunId;
	}

	public String reviewText() {
		return reviewText;
	}

	public int implementationReworkCount() {
		return implementationReworkCount;
	}

	public int reviewReworkCount() {
		return reviewReworkCount;
	}

	public String commitId() {
		return commitId;
	}

	public String branchUrl() {
		return branchUrl;
	}

	public long prNumber() {
		return prNumber;
	}

	public String prUrl() {
		return prUrl;
	}

	public String failureReason() {
		return failureReason;
	}

	/**
	 * Переводит только что созданный Workflow в состояние исполнения.
	 */
	public void markRunning() {
		if (status == WorkflowStatus.CREATED) {
			status = WorkflowStatus.RUNNING;
		}
	}

	/**
	 * Фиксирует результат анализа Analyst (текст и предложенное имя ветки).
	 *
	 * @param branchName предложенное имя ветки
	 * @param analysisText текст артефакта анализа
	 */
	public void recordAnalysis(String branchName, String analysisText) {
		if (currentStage != WorkflowStage.ANALYZING_REQUIREMENTS) {
			throw new IllegalWorkflowTransitionException("cannot record analysis from stage " + currentStage);
		}
		if (status != WorkflowStatus.CREATED && status != WorkflowStatus.RUNNING) {
			throw new IllegalWorkflowTransitionException("cannot record analysis from status " + status);
		}
		this.branchName = branchName;
		this.analysisText = analysisText;
		appendStep("ANALYZING_REQUIREMENTS", "ANALYST", "ANALYZE", analysisText, null);
		checkpoints.add(new Checkpoint("analysis", branchName == null ? "" : branchName));
		waitForUser();
	}

	/**
	 * Продолжает Workflow после успешного анализа на следующий этап ровно один раз.
	 */
	public void continueAfterAnalysis() {
		requireWaitingAt(WorkflowStage.ANALYZING_REQUIREMENTS);
		if (analysisText == null || branchName == null) {
			throw new IllegalWorkflowTransitionException("cannot continue without a recorded analysis result");
		}
		resume();
		completeStage();
		this.analysisText = null;
		this.designText = null;
	}

	/**
	 * Возвращает Workflow к анализу для повторного запуска с замечаниями.
	 *
	 * @param feedback замечания пользователя к текущему анализу
	 */
	public void rework(String feedback) {
		requireWaitingAt(WorkflowStage.ANALYZING_REQUIREMENTS);
		if (analysisText == null) {
			throw new IllegalWorkflowTransitionException("cannot rework without a recorded result");
		}
		resume();
		this.analysisText = null;
		this.branchName = null;
		appendStep(currentStage.name(), roleForStage(), "REWORK", feedback, null);
		checkpoints.add(new Checkpoint("rework", feedback == null ? "" : feedback));
	}

	/**
	 * Фиксирует результат проектирования Architect.
	 *
	 * @param designText текст design-артефакта
	 */
	public void recordDesign(String designText) {
		if (currentStage != WorkflowStage.DESIGNING_SOLUTION) {
			throw new IllegalWorkflowTransitionException("cannot record design from stage " + currentStage);
		}
		this.designText = designText;
		appendStep("DESIGNING_SOLUTION", "ARCHITECT", "DESIGN", designText, null);
		checkpoints.add(new Checkpoint("design", ""));
		waitForUser();
	}

	/**
	 * Продолжает Workflow после утверждённого proposal Architect на этап реализации.
	 */
	public void continueAfterDesign() {
		requireWaitingAt(WorkflowStage.DESIGNING_SOLUTION);
		if (designText == null) {
			throw new IllegalWorkflowTransitionException("cannot continue without an approved proposal");
		}
		resume();
		completeStage();
		this.designText = null;
	}

	/**
	 * Возвращает Workflow к проектированию для повторного запуска с замечаниями.
	 *
	 * @param feedback замечания пользователя к текущему проекту
	 */
	public void reworkDesign(String feedback) {
		requireWaitingAt(WorkflowStage.DESIGNING_SOLUTION);
		if (designText == null) {
			throw new IllegalWorkflowTransitionException("cannot rework without a recorded result");
		}
		resume();
		this.designText = null;
		appendStep(currentStage.name(), roleForStage(), "REWORK", feedback, null);
		checkpoints.add(new Checkpoint("rework", feedback == null ? "" : feedback));
	}

	/**
	 * Фиксирует результат реализации Developer.
	 *
	 * <p>При успехе переводит этап на {@link WorkflowStage#REVIEWING}; при неудаче
	 * переводит workflow в {@code WAITING_FOR_USER} для повторной итерации.</p>
	 *
	 * @param runId идентификатор запуска агента
	 * @param diagnostics диагностика сборки/тестов
	 * @param success признак успешной сборки и тестов
	 */
	public void recordImplementation(String runId, String diagnostics, boolean success) {
		if (currentStage != WorkflowStage.IMPLEMENTING) {
			throw new IllegalWorkflowTransitionException("cannot record implementation from stage " + currentStage);
		}
		this.implementationRunId = runId;
		this.implementationDiagnostics = diagnostics;
		appendStep("IMPLEMENTING", "DEVELOPER", "IMPLEMENT", diagnostics, runId);
		if (success) {
			if (status == WorkflowStatus.WAITING_FOR_USER) {
				resume();
			}
			completeStage();
			checkpoints.add(new Checkpoint("implementation", diagnostics == null ? "" : diagnostics));
		} else {
			checkpoints.add(new Checkpoint("implementation-failure", diagnostics == null ? "" : diagnostics));
			waitForUser();
		}
	}

	/**
	 * Возвращает Workflow к реализации с замечаниями пользователя (лимит 3).
	 *
	 * @param feedback замечания пользователя к текущей реализации
	 */
	public void reworkImplementation(String feedback) {
		requireWaitingAt(WorkflowStage.IMPLEMENTING);
		if (implementationReworkCount >= MAX_REWORK) {
			fail("implementation rework limit exceeded");
			return;
		}
		resume();
		implementationReworkCount++;
		this.implementationDiagnostics = null;
		this.implementationRunId = null;
		appendStep("IMPLEMENTING", "DEVELOPER", "REWORK", feedback, null);
		checkpoints.add(new Checkpoint("rework", feedback == null ? "" : feedback));
	}

	/**
	 * Фиксирует результат независимого ревью Reviewer.
	 *
	 * @param runId идентификатор запуска агента
	 * @param reviewText текст результата ревью
	 */
	public void recordReview(String runId, String reviewText) {
		if (currentStage != WorkflowStage.REVIEWING) {
			throw new IllegalWorkflowTransitionException("cannot record review from stage " + currentStage);
		}
		this.reviewRunId = runId;
		this.reviewText = reviewText;
		appendStep("REVIEWING", "REVIEWER", "REVIEW", reviewText, runId);
		checkpoints.add(new Checkpoint("review", ""));
		waitForUser();
	}

	/**
	 * Одобряет результат ревью и переводит workflow к финализации.
	 */
	public void approveReview() {
		requireWaitingAt(WorkflowStage.REVIEWING);
		if (reviewText == null) {
			throw new IllegalWorkflowTransitionException("cannot approve without a recorded review result");
		}
		resume();
		completeStage();
		this.reviewText = null;
	}

	/**
	 * Возвращает замечания ревью в реализацию (лимит 3).
	 *
	 * @param feedback замечания пользователя к результату ревью
	 */
	public void reworkReview(String feedback) {
		requireWaitingAt(WorkflowStage.REVIEWING);
		if (reviewReworkCount >= MAX_REWORK) {
			fail("review rework limit exceeded");
			return;
		}
		resume();
		reviewReworkCount++;
		this.reviewText = null;
		this.reviewRunId = null;
		this.currentStage = WorkflowStage.IMPLEMENTING;
		appendStep("REVIEWING", "REVIEWER", "REWORK", feedback, null);
		checkpoints.add(new Checkpoint("rework", feedback == null ? "" : feedback));
	}

	/**
	 * Фиксирует успешную финализацию (коммит, публикация ветки, PR) и завершает workflow.
	 */
	public void finalize(String commitId, String branchUrl, long prNumber, String prUrl) {
		if (currentStage != WorkflowStage.READY_FOR_PULL_REQUEST) {
			throw new IllegalWorkflowTransitionException("cannot finalize from stage " + currentStage);
		}
		this.commitId = commitId;
		this.branchUrl = branchUrl;
		this.prNumber = prNumber;
		this.prUrl = prUrl;
		completedStages.add(currentStage);
		status = WorkflowStatus.COMPLETED;
		checkpoints.add(new Checkpoint("finalize", prUrl == null ? "" : prUrl));
	}

	/**
	 * Завершает текущий этап и переходит на следующий допустимый этап.
	 *
	 * @return новый текущий этап
	 * @throws IllegalWorkflowTransitionException если текущий этап не является завершаемым
	 */
	public WorkflowStage completeStage() {
		if (status != WorkflowStatus.CREATED && status != WorkflowStatus.RUNNING) {
			throw new IllegalWorkflowTransitionException(
					"cannot complete stage from status " + status);
		}
		Set<WorkflowStage> next = NEXT_STAGES.getOrDefault(currentStage, Set.of());
		if (next.isEmpty()) {
			throw new IllegalWorkflowTransitionException(
					"stage " + currentStage + " has no allowed next stage in status " + status);
		}
		return completeStage(next.iterator().next());
	}

	/**
	 * Завершает текущий этап и переходит к явно указанному следующему этапу.
	 *
	 * @param next целевой этап
	 * @return новый текущий этап
	 * @throws IllegalWorkflowTransitionException если переход недопустим
	 */
	public WorkflowStage completeStage(WorkflowStage next) {
		Set<WorkflowStage> allowed = NEXT_STAGES.getOrDefault(currentStage, Set.of());
		if (!allowed.contains(next)) {
			throw new IllegalWorkflowTransitionException(
					"illegal transition from " + currentStage + " to " + next + " in status " + status);
		}
		completedStages.add(currentStage);
		currentStage = next;
		if (status == WorkflowStatus.CREATED) {
			status = WorkflowStatus.RUNNING;
		}
		return currentStage;
	}

	/**
	 * Переводит Workflow в состояние ожидания решения пользователя.
	 */
	public void waitForUser() {
		if (status == WorkflowStatus.CREATED || status == WorkflowStatus.RUNNING) {
			status = WorkflowStatus.WAITING_FOR_USER;
			return;
		}
		throw new IllegalWorkflowTransitionException("cannot wait for user from status " + status);
	}

	/**
	 * Возвращает Workflow из ожидания решения пользователя в исполнение.
	 */
	public void resume() {
		if (status == WorkflowStatus.WAITING_FOR_USER) {
			status = WorkflowStatus.RUNNING;
			return;
		}
		throw new IllegalWorkflowTransitionException("cannot resume from status " + status);
	}

	/**
	 * Помечает Workflow как завершившийся ошибкой.
	 */
	public void fail(String reason) {
		status = WorkflowStatus.FAILED;
		failureReason = reason == null ? "" : reason;
		checkpoints.add(new Checkpoint("failure", reason == null ? "" : reason));
	}

	/**
	 * Отменяет Workflow.
	 */
	public void cancel() {
		if (status == WorkflowStatus.COMPLETED) {
			throw new IllegalWorkflowTransitionException("cannot cancel completed workflow");
		}
		status = WorkflowStatus.CANCELLED;
	}

	/**
	 * Связывает Workflow с идентификатором запуска Temporal.
	 */
	public void attachExecution(String executionId) {
		this.executionId = executionId;
		checkpoints.add(new Checkpoint("execution", executionId == null ? "" : executionId));
	}

	public void addCheckpoint(String name, String value) {
		checkpoints.add(new Checkpoint(name, value));
	}

	private void requireWaitingAt(WorkflowStage stage) {
		if (status != WorkflowStatus.WAITING_FOR_USER) {
			throw new IllegalWorkflowTransitionException("cannot act from status " + status);
		}
		if (currentStage != stage) {
			throw new IllegalWorkflowTransitionException("cannot act from stage " + currentStage);
		}
	}

	private String roleForStage() {
		return switch (currentStage) {
			case ANALYZING_REQUIREMENTS -> "ANALYST";
			case DESIGNING_SOLUTION -> "ARCHITECT";
			case IMPLEMENTING -> "DEVELOPER";
			case REVIEWING -> "REVIEWER";
			default -> currentStage.name();
		};
	}

	private static Map<WorkflowStage, Set<WorkflowStage>> nextStages() {
		Map<WorkflowStage, Set<WorkflowStage>> map = new EnumMap<>(WorkflowStage.class);
		map.put(WorkflowStage.ANALYZING_REQUIREMENTS, EnumSet.of(WorkflowStage.DESIGNING_SOLUTION));
		map.put(WorkflowStage.DESIGNING_SOLUTION, EnumSet.of(WorkflowStage.IMPLEMENTING));
		map.put(WorkflowStage.IMPLEMENTING, EnumSet.of(WorkflowStage.REVIEWING));
		map.put(WorkflowStage.REVIEWING, EnumSet.of(WorkflowStage.READY_FOR_PULL_REQUEST));
		map.put(WorkflowStage.READY_FOR_PULL_REQUEST, EnumSet.noneOf(WorkflowStage.class));
		return Collections.unmodifiableMap(map);
	}
}
