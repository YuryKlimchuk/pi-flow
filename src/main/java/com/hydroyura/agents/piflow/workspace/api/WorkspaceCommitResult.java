package com.hydroyura.agents.piflow.workspace.api;

import java.nio.file.Path;

/**
 * Результат локального коммита.
 *
 * @param path путь к рабочей директории
 * @param branch ветка, на которой выполнен коммит
 * @param commitId идентификатор созданного коммита
 */
public record WorkspaceCommitResult(Path path, String branch, String commitId) {
}
