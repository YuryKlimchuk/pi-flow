package com.hydroyura.agents.piflow.workflow.infrastructure.temporal;

/**
 * Результат activity финализации.
 *
 * @param commitId идентификатор итогового коммита
 * @param branchUrl URL опубликованной ветки
 * @param prNumber номер созданного pull request
 * @param prUrl URL созданного pull request
 */
public record WorkflowFinalizationResult(String commitId, String branchUrl, long prNumber, String prUrl) {
}
