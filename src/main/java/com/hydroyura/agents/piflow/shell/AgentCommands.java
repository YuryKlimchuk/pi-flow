package com.hydroyura.agents.piflow.shell;

import com.hydroyura.agents.piflow.agent.PiRunResult;
import com.hydroyura.agents.piflow.agent.PiRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.CommandGroup;

/** Interactive commands to exercise the Pi agent integration manually. */
@CommandGroup
public class AgentCommands {

    private static final Logger log = LoggerFactory.getLogger(AgentCommands.class);

    private final PiRunner piRunner;

    public AgentCommands(PiRunner piRunner) {
        this.piRunner = piRunner;
    }

    @Command(name = "pi-run", description = "Run a prompt against Pi in JSON mode")
    public String piRun(@Argument(index = 0, description = "Prompt text") String prompt) {
        PiRunResult result = piRunner.run(prompt);
        log.info("pi-run completed: status={}, finalText={}, diagnostics={}",
                result.status(), result.finalText(), result.diagnostics());
        return "status=" + result.status()
                + "\nfinalText=" + result.finalText()
                + "\ndiagnostics=" + result.diagnostics();
    }
}
