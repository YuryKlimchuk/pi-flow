package com.hydroyura.agents.piflow.workflow.infrastructure;

import com.hydroyura.agents.piflow.workflow.application.ArtifactFingerprint;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * SHA-256 artifact fingerprint over a change directory's deterministic file set.
 *
 * <p>Covered files, in order: {@code proposal.md}, {@code design.md}, every regular file under
 * {@code specs/} sorted by relative path, then {@code tasks.md}. {@code tasks.md} checkbox markers
 * ({@code - [ ]} and {@code - [x]}) are normalized to a fixed placeholder so checkbox-only edits do
 * not change the digest; every other byte of every covered file is included.
 */
@Component
public class ChangeArtifactFingerprint implements ArtifactFingerprint {

    private static final String CHECKBOX_PLACEHOLDER = "- [_]";

    @Override
    public String compute(Path changeDir) {
        Objects.requireNonNull(changeDir, "changeDir must not be null");
        MessageDigest digest = newSha256();
        for (Path file : collectFiles(changeDir)) {
            String relative = normalizeRelative(changeDir.relativize(file));
            digest.update(relative.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(readNormalized(file, relative.endsWith("tasks.md")));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static List<Path> collectFiles(Path changeDir) {
        List<Path> files = new ArrayList<>();
        addIfExists(files, changeDir.resolve("proposal.md"));
        addIfExists(files, changeDir.resolve("design.md"));

        Path specsDir = changeDir.resolve("specs");
        if (Files.isDirectory(specsDir)) {
            try (Stream<Path> stream = Files.walk(specsDir)) {
                stream.filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(changeDir::relativize))
                        .forEach(files::add);
            } catch (IOException e) {
                throw new UncheckedIOException("failed to walk specs directory " + specsDir, e);
            }
        }

        addIfExists(files, changeDir.resolve("tasks.md"));
        return files;
    }

    private static void addIfExists(List<Path> files, Path file) {
        if (Files.isRegularFile(file)) {
            files.add(file);
        }
    }

    private static byte[] readNormalized(Path file, boolean normalizeCheckboxes) {
        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read " + file, e);
        }
        if (normalizeCheckboxes) {
            content = content.replace("- [ ]", CHECKBOX_PLACEHOLDER).replace("- [x]", CHECKBOX_PLACEHOLDER);
        }
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private static String normalizeRelative(Path relative) {
        return relative.toString().replace('\\', '/');
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
