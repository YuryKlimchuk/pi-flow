package com.hydroyura.agents.piflow.workspace.domain;

import java.time.Instant;

/**
 * Активный lease блокировки (внутренняя модель).
 */
public record Lease(String key, String owner, String token, Instant expiresAt) {
}
