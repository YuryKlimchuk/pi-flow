package com.hydroyura.agents.piflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PiFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(PiFlowApplication.class, args);
	}

}