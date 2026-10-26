package com.hydroyura.agents.piflow.workspace.api;

import java.nio.file.Path;

/**
 * Результат создания итоговой ветки.
 *
 * @param path путь к рабочей директории
 * @param branch имя итоговой ветки
 */
public record WorkspaceBranchResult(Path path, String branch) {
}
