package com.hydroyura.agents.piflow.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Resolves agent names to {@link AgentDefinition}s by scanning agent directories in order. */
public class AgentResolver {

    private static final Logger log = LoggerFactory.getLogger(AgentResolver.class);

    private final List<Path> agentDirs;

    public AgentResolver(List<Path> agentDirs) {
        this.agentDirs = List.copyOf(agentDirs);
    }

    public Optional<AgentDefinition> resolve(String name) {
        for (Path dir : agentDirs) {
            Path file = dir.resolve(name + ".md");
            if (!Files.isRegularFile(file)) {
                continue;
            }
            try {
                String content = Files.readString(file);
                return Optional.of(AgentFileParser.parse(name, content));
            } catch (IOException e) {
                log.warn("Failed to read agent file {}: {}", file, e.getMessage());
            }
        }
        return Optional.empty();
    }
}
