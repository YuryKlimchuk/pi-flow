package com.hydroyura.agents.piflow.agent.infrastructure;

import com.hydroyura.agents.piflow.agent.application.AgentResolution;
import com.hydroyura.agents.piflow.agent.application.AgentResolver;
import com.hydroyura.agents.piflow.agent.domain.AgentDefinition;
import com.hydroyura.agents.piflow.agent.domain.Skill;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Resolves agents from orchestrator resources ({@code agents/<name>/system.md} and
 * {@code agents/<name>/skills/*.md}) plus the configured tier mapping.
 */
@Slf4j
public class ResourceAgentResolver implements AgentResolver {

    private static final String RESOURCE_ROOT = "classpath:agents/";

    private final AgentProperties properties;
    private final ResourcePatternResolver resourceResolver;

    public ResourceAgentResolver(AgentProperties properties, ResourcePatternResolver resourceResolver) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.resourceResolver = Objects.requireNonNull(resourceResolver, "resourceResolver must not be null");
    }

    @Override
    public AgentResolution resolve(String name) {
        Objects.requireNonNull(name, "name must not be null");

        String tier = properties.tierFor(name);
        if (tier == null || tier.isBlank()) {
            return AgentResolution.error("No tier configured for agent: " + name);
        }

        String model = properties.modelFor(tier);
        if (model == null || model.isBlank()) {
            return AgentResolution.error("No model configured for tier: " + tier);
        }

        SystemPromptParser.SystemPrompt prompt = readSystemPrompt(name);
        if (prompt == null) {
            return AgentResolution.error("No system prompt found for agent: " + name);
        }

        List<Skill> skills = readSkills(name);
        return AgentResolution.ok(new AgentDefinition(name, model, prompt.tools(), prompt.body(), skills));
    }

    private SystemPromptParser.SystemPrompt readSystemPrompt(String name) {
        Resource resource = resourceResolver.getResource(RESOURCE_ROOT + name + "/system.md");
        if (!resource.exists()) {
            return null;
        }
        String content = readAsString(resource);
        return content == null ? null : SystemPromptParser.parse(content);
    }

    private List<Skill> readSkills(String name) {
        List<Skill> skills = new ArrayList<>();
        try {
            Resource[] resources = resourceResolver.getResources(RESOURCE_ROOT + name + "/skills/*.md");
            for (Resource resource : resources) {
                String content = readAsString(resource);
                if (content == null) {
                    continue;
                }
                String skillName = resource.getFilename() == null
                        ? "skill"
                        : resource.getFilename().replaceAll("\\.md$", "");
                skills.add(new Skill(skillName, content));
            }
        } catch (IOException e) {
            log.warn("Failed to list skills for agent {}: {}", name, e.getMessage());
        }
        return skills;
    }

    private String readAsString(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to read agent resource {}: {}", resource, e.getMessage());
            return null;
        }
    }
}
