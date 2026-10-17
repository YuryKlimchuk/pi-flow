package com.hydroyura.agents.piflow.agent;

/** Port for invoking the Pi Coding Agent and capturing a structured result. */
public interface PiRunner {

    PiRunResult run(String prompt);

    PiRunResult run(String agentName, String prompt);
}
