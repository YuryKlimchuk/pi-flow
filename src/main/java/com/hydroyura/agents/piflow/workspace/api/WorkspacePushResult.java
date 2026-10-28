package com.hydroyura.agents.piflow.workspace.api;

import java.nio.file.Path;

/**
 * Результат публикации (push) локальной ветки.
 *
 * @param path путь к рабочей директории
 * @param branch имя опубликованной ветки
 */
public record WorkspacePushResult(Path path, String branch) {
}
