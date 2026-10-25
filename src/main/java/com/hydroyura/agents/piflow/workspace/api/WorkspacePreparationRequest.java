package com.hydroyura.agents.piflow.workspace.api;

import java.util.Map;

/**
 * Входная модель подготовки рабочего пространства.
 *
 * @param workflowId идентификатор Workflow
 * @param repositoryUrl адрес репозитория
 * @param targetBranch целевая ветка
 * @param placementParameters параметры размещения
 */
public record WorkspacePreparationRequest(
		String workflowId,
		String repositoryUrl,
		String targetBranch,
		Map<String, String> placementParameters) {

	public WorkspacePreparationRequest {
		placementParameters = placementParameters == null ? Map.of() : Map.copyOf(placementParameters);
	}

	/**
	 * Проверяет обязательные поля и синтаксис ветки до Git-операции.
	 *
	 * @throws WorkspaceException если входные данные невалидны
	 */
	public void validate() {
		requireText(workflowId, "workflowId");
		requireText(repositoryUrl, "repositoryUrl");
		requireText(targetBranch, "targetBranch");
		if (!isValidBranchName(targetBranch)) {
			throw WorkspaceException.validation("invalid branch name: " + targetBranch);
		}
	}

	private static void requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw WorkspaceException.validation(field + " must not be blank");
		}
	}

	private static boolean isValidBranchName(String name) {
		if (name.startsWith("/") || name.endsWith("/") || name.endsWith(".")) {
			return false;
		}
		if (name.contains("..") || name.contains("@{") || name.contains("//")) {
			return false;
		}
		return name.chars().allMatch(c -> c > 0x20 && c != 0x7f
				&& c != '~' && c != '^' && c != ':' && c != '?' && c != '*' && c != '[' && c != '\\');
	}
}
