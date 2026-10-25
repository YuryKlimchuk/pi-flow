package com.hydroyura.agents.piflow.workspace.api;

import java.time.Instant;

/**
 * Эксклюзивная блокировка локального репозитория.
 *
 * @param repositoryKey стабильный ключ репозитория
 * @param owner владелец блокировки
 * @param token токен владельца для освобождения
 * @param expiresAt срок действия lease
 */
public record WorkspaceLock(String repositoryKey, String owner, String token, Instant expiresAt) {
}
