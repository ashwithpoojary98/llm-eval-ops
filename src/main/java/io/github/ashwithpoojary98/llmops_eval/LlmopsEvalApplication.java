package io.github.ashwithpoojary98.llmops_eval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LlmopsEvalApplication {

	public static void main(String[] args) {
		SpringApplication.run(LlmopsEvalApplication.class, args);
	}

}
