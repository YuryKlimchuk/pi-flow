package com.hydroyura.agents.piflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PiFlowApplication {

	static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(PiFlowApplication.class, args);
		System.exit(SpringApplication.exit(context));
	}

}