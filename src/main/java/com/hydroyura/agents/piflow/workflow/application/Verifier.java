package com.hydroyura.agents.piflow.workflow.application;

import com.hydroyura.agents.piflow.workflow.domain.Workflow;

/** Port for the deterministic automated verification stage. */
public interface Verifier {

    boolean verify(Workflow workflow);
}
