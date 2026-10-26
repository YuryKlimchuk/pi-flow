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

	private final WorkflowId id;
	private final WorkflowContext context;
	private WorkflowStatus status;
	private WorkflowStage currentStage;
	private final List<WorkflowStage> completedStages = new ArrayList<>();
	private final List<Checkpoint> checkpoints = new ArrayList<>();
	private String executionId;
	private String branchName;
	private String analysisText;
	private String designText;
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
		checkpoints.add(new Checkpoint("analysis", branchName == null ? "" : branchName));
		waitForUser();
	}

	/**
	 * Продолжает Workflow после успешного анализа на следующий этап ровно один раз.
	 */
	public void continueAfterAnalysis() {
		if (status != WorkflowStatus.WAITING_FOR_USER) {
			throw new IllegalWorkflowTransitionException("cannot continue from status " + status);
		}
		boolean hasAnalysis = currentStage == WorkflowStage.ANALYZING_REQUIREMENTS
				&& analysisText != null && branchName != null;
		boolean hasDesign = currentStage == WorkflowStage.DESIGNING_SOLUTION && designText != null;
		if (!hasAnalysis && !hasDesign) {
			throw new IllegalWorkflowTransitionException("cannot continue without a recorded result");
		}
		resume();
		completeStage();
		this.analysisText = null;
		this.branchName = null;
		this.designText = null;
	}

	/**
	 * Возвращает Workflow к анализу для повторного запуска с замечаниями.
	 *
	 * @param feedback замечания пользователя к текущему анализу
	 */
	public void rework(String feedback) {
		if (status != WorkflowStatus.WAITING_FOR_USER) {
			throw new IllegalWorkflowTransitionException("cannot rework from status " + status);
		}
		if (analysisText == null && designText == null) {
			throw new IllegalWorkflowTransitionException("cannot rework without a recorded result");
		}
		resume();
		this.analysisText = null;
		this.branchName = null;
		this.designText = null;
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
		checkpoints.add(new Checkpoint("design", ""));
		waitForUser();
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
		// Единственный следующий этап по линейной модели процесса.
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
	 * Помечает Workflow как завершённый. Допустимо только с последнего этапа.
	 */
	public void complete() {
		if (currentStage != WorkflowStage.READY_FOR_PULL_REQUEST) {
			throw new IllegalWorkflowTransitionException(
					"cannot complete workflow from stage " + currentStage);
		}
		if (status == WorkflowStatus.RUNNING || status == WorkflowStatus.CREATED || status == WorkflowStatus.WAITING_FOR_USER) {
			completedStages.add(currentStage);
			currentStage = WorkflowStage.READY_FOR_PULL_REQUEST;
			status = WorkflowStatus.COMPLETED;
			return;
		}
		throw new IllegalWorkflowTransitionException("cannot complete workflow from status " + status);
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
