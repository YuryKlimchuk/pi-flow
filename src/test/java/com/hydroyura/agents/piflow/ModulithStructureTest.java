package com.hydroyura.agents.piflow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTest {

    @Test
    void verifyModuleStructure() {
        ApplicationModules.of(PiFlowApplication.class).verify();
    }
}
