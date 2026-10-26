package com.hydroyura.agents.piflow.agent.domain;

import java.nio.file.Path;

/**
 * Внутренний driven-порт резолва роль-ресурсов агента.
 *
 * <p>Предоставляет файловые пути системного промпта и каталога скилов роли,
 * используемые при сборке командной строки Pi. Реализация скрывает источник
 * ресурсов (classpath, рабочее дерево или извлечённая копия).</p>
 */
public interface RoleResources {

	/**
	 * Возвращает путь к файлу системного промпта роли.
	 *
	 * @param role идентификатор роли (например, {@code ANALYST})
	 * @return путь к файлу системного промпта
	 * @throws AgentProcessException если ресурс не найден или не может быть материализован
	 */
	Path systemPromptFile(String role);

	/**
	 * Возвращает путь к каталогу скилов роли.
	 *
	 * @param role идентификатор роли (например, {@code ANALYST})
	 * @return путь к каталогу скилов
	 * @throws AgentProcessException если ресурс не найден или не может быть материализован
	 */
	Path skillsDirectory(String role);
}
