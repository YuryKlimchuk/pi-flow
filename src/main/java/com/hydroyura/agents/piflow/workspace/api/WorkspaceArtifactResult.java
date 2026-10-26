package com.hydroyura.agents.piflow.workspace.api;

/**
 * Результат чтения артефакта анализа из подготовленного workspace.
 *
 * @param text полный текст артефакта
 * @param path путь к артефакту
 * @param size размер артефакта в байтах
 */
public record WorkspaceArtifactResult(String text, String path, long size) {
}
