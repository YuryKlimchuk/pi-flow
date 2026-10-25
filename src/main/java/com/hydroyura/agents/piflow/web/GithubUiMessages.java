package com.hydroyura.agents.piflow.web;

import com.hydroyura.agents.piflow.github.api.GithubException;

/**
 * Преобразует типизированные ошибки GitHub в безопасные сообщения для UI.
 *
 * <p>Сообщения не содержат PAT, стектрейсов и деталей инфраструктуры.</p>
 */
public final class GithubUiMessages {

	private GithubUiMessages() {
	}

	public static String message(GithubException exception) {
		return switch (exception.errorType()) {
			case CONFIGURATION -> "GitHub не настроен: задайте переменную окружения GITHUB_PI_FLOW_TOKEN.";
			case AUTHENTICATION -> "Не удалось авторизоваться в GitHub: проверьте Personal Access Token (PAT).";
			case NOT_FOUND -> "Ресурс GitHub не найден.";
			case CONFLICT -> "Конфликт при обращении к GitHub.";
			case VALIDATION -> "Некорректные параметры запроса.";
			case RATE_LIMIT -> "Превышен лимит запросов GitHub API. Повторите позже.";
			case SERVER -> "GitHub временно недоступен. Повторите позже.";
			case NETWORK -> "Не удалось подключиться к GitHub. Проверьте сеть и повторите.";
		};
	}
}
