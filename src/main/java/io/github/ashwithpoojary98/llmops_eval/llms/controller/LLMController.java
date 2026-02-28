package io.github.ashwithpoojary98.llmops_eval.llms.controller;

import io.github.ashwithpoojary98.llmops_eval.llms.dto.RegisterLLMRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/llms")
@Slf4j
public class LLMController {


    @PostMapping("/registerEvalLLM")
    public String registerEvalLLM(@RequestBody RegisterLLMRequest request) {
        log.info("Received request to register LLM: {}", request);
        return "LLM registered successfully!";

    }
}
