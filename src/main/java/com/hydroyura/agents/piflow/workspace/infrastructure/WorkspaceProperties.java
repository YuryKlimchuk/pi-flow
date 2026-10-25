package com.hydroyura.agents.piflow.workspace.infrastructure;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed-конфигурация размещения рабочих копий и блокировок.
 *
 * @param baseDirectory базовая директория рабочих копий
 * @param lockDirectory директория хранения файловых блокировок
 * @param leaseDuration срок действия lease блокировки
 */
@ConfigurationProperties(prefix = "workspace")
public record WorkspaceProperties(
		Path baseDirectory,
		Path lockDirectory,
		@DefaultValue("5m") Duration leaseDuration) {
}
