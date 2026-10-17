package com.hydroyura.agents.piflow.agent.infrastructure;

import com.hydroyura.agents.piflow.agent.application.AgentResolver;
import com.hydroyura.agents.piflow.agent.domain.AgentDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/** Resolves agent names to definitions by scanning agent directories in order. */
@Slf4j
public class FileAgentResolver implements AgentResolver {

    private final List<Path> agentDirs;

    public FileAgentResolver(List<Path> agentDirs) {
        this.agentDirs = List.copyOf(agentDirs);
    }

    @Override
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
