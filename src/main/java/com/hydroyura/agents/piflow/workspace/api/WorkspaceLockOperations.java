package com.hydroyura.agents.piflow.workspace.api;

/**
 * Публичный порт жизненного цикла эксклюзивной блокировки репозитория.
 */
public interface WorkspaceLockOperations {

	/**
	 * Получает эксклюзивную блокировку репозитория.
	 *
	 * @param repositoryUrl адрес репозитория
	 * @param owner владелец блокировки
	 * @return полученный lease
	 */
	WorkspaceLock acquire(String repositoryUrl, String owner);

	/**
	 * Освобождает блокировку по токену владельца.
	 *
	 * @param lock полученная ранее блокировка
	 */
	void release(WorkspaceLock lock);
}
