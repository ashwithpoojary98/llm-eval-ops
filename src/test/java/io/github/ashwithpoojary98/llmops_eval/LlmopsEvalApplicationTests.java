package io.github.ashwithpoojary98.llmops_eval;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        // Disable mail auto-configuration — no SMTP server available in CI
        "spring.mail.host=",
        // Prevent scheduler from firing during test context load
        "spring.task.scheduling.pool.size=0",
        // Point eval engine at a dummy URL — nothing calls it during context load
        "evaluation.engine.url=http://localhost:9999"
})
class LlmopsEvalApplicationTests {

    @Test
    void contextLoads() {
    }

}
