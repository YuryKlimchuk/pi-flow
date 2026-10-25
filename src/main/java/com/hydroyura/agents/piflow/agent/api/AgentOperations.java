package com.hydroyura.agents.piflow.agent.api;

/**
 * Публичный порт запуска технической задачи Pi-агента.
 */
public interface AgentOperations {

	/**
	 * Запускает задачу агента и ожидает её завершения.
	 *
	 * @param command параметры запуска
	 * @return технический результат выполнения
	 */
	AgentRunResult run(StartAgentCommand command);

	/**
	 * Отменяет активный запуск по его идентификатору.
	 *
	 * @param runId идентификатор запуска
	 */
	void cancel(String runId);
}
