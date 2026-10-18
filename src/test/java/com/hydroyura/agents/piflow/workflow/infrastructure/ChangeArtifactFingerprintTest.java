package com.hydroyura.agents.piflow.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChangeArtifactFingerprintTest {

    @TempDir
    Path tempDir;

    private final ChangeArtifactFingerprint fingerprint = new ChangeArtifactFingerprint();

    @Test
    void sameContents_shouldProduceSameDigest() throws IOException {
        Path changeDir = createChange();

        assertThat(fingerprint.compute(changeDir)).isEqualTo(fingerprint.compute(changeDir));
    }

    @Test
    void modifyingNonCheckboxContent_shouldChangeDigest() throws IOException {
        Path changeDir = createChange();
        String before = fingerprint.compute(changeDir);

        Files.writeString(changeDir.resolve("proposal.md"), "# Proposal\n\nWhy: edited proposal\n");

        assertThat(fingerprint.compute(changeDir)).isNotEqualTo(before);
    }

    @Test
    void addingAFile_shouldChangeDigest() throws IOException {
        Path changeDir = createChange();
        String before = fingerprint.compute(changeDir);

        Files.writeString(changeDir.resolve("specs/extra.md"), "# Extra\n\n## ADDED Requirements\n");

        assertThat(fingerprint.compute(changeDir)).isNotEqualTo(before);
    }

    @Test
    void removingAFile_shouldChangeDigest() throws IOException {
        Path changeDir = createChange();
        String before = fingerprint.compute(changeDir);

        Files.delete(changeDir.resolve("design.md"));

        assertThat(fingerprint.compute(changeDir)).isNotEqualTo(before);
    }

    @Test
    void togglingCheckboxInTasksMd_shouldNotChangeDigest() throws IOException {
        Path changeDir = createChange();
        String before = fingerprint.compute(changeDir);

        Files.writeString(changeDir.resolve("tasks.md"), "- [x] 1.1 Do a thing\n- [ ] 1.2 Another thing\n");

        assertThat(fingerprint.compute(changeDir)).isEqualTo(before);
    }

    @Test
    void editingNonCheckboxTextInTasksMd_shouldChangeDigest() throws IOException {
        Path changeDir = createChange();
        String before = fingerprint.compute(changeDir);

        Files.writeString(changeDir.resolve("tasks.md"), "- [ ] 1.1 Do a completely different thing\n");

        assertThat(fingerprint.compute(changeDir)).isNotEqualTo(before);
    }

    private Path createChange() throws IOException {
        Path changeDir = Files.createDirectories(tempDir.resolve("openspec/changes/my-change"));
        Files.writeString(changeDir.resolve("proposal.md"), "# Proposal\n\nWhy: original\n");
        Files.writeString(changeDir.resolve("design.md"), "# Design\n\nContext: original\n");
        Files.writeString(changeDir.resolve("tasks.md"), "- [ ] 1.1 Do a thing\n- [ ] 1.2 Another thing\n");
        Path specs = Files.createDirectories(changeDir.resolve("specs/cap"));
        Files.writeString(specs.resolve("spec.md"),
                "# Spec Delta\n\n## ADDED Requirements\n\n### Requirement: Thing\nBody\n\n#### Scenario: S\n- **WHEN** x\n- **THEN** y\n");
        return changeDir;
    }
}
