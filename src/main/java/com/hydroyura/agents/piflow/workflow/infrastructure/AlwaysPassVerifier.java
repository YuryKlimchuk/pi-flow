package com.hydroyura.agents.piflow.workflow.infrastructure;

import com.hydroyura.agents.piflow.workflow.application.Verifier;
import com.hydroyura.agents.piflow.workflow.domain.Workflow;
import org.springframework.stereotype.Component;

/** Placeholder verification that always passes. Real checks are a follow-up. */
@Component
public class AlwaysPassVerifier implements Verifier {

    @Override
    public boolean verify(Workflow workflow) {
        return true;
    }
}
