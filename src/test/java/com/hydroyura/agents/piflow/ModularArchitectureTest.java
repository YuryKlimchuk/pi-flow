package com.hydroyura.agents.piflow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Проверяет целостность границ модулей и отсутствие циклов.
 */
class ModularArchitectureTest {

	private final ApplicationModules modules = ApplicationModules.of(PiFlowApplication.class);

	@Test
	void verifyModularStructure() {
		modules.verify();
	}
}
