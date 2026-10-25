package com.hydroyura.agents.piflow.agent.domain;

/**
 * Внутренний driven-порт запуска процесса Pi.
 *
 * <p>Реализуется инфраструктурным адаптером; application-слой модуля зависит только
 * от этого порта и преобразует его результаты и ошибки в публичные типы.</p>
 */
public interface AgentProcessRunner {

	AgentProcessResult execute(AgentProcessRequest request);

	void cancel(String runId);
}
