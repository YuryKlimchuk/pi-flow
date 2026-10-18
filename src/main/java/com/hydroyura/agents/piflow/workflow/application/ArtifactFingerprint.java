package com.hydroyura.agents.piflow.workflow.application;

import java.nio.file.Path;

/**
 * Port for computing the deterministic artifact fingerprint of a bound change directory.
 *
 * <p>The fingerprint covers {@code proposal.md}, {@code design.md}, every file under {@code specs/},
 * and {@code tasks.md} with its checkbox markers normalized so checkbox-only edits do not change the digest.
 */
public interface ArtifactFingerprint {

    String compute(Path changeDir);
}
