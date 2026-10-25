package com.hydroyura.agents.piflow.workspace.api;

import java.nio.file.Path;

/**
 * Результат подготовки рабочего пространства.
 *
 * @param path путь к рабочей директории
 * @param repositoryUrl адрес репозитория
 * @param branch фактически выбранная ветка
 * @param state состояние рабочего дерева
 */
public record WorkspaceResult(Path path, String repositoryUrl, String branch, WorkspaceState state) {
}
