package io.github.ashwithpoojary98.llmops_eval;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        // Disable mail auto-configuration — no SMTP server in CI
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration," +
                "org.springframework.boot.autoconfigure.mail.MailSenderValidatorAutoConfiguration",
        // Skip admin bootstrap — no need to seed DB during context load test
        "llmops.bootstrap.admin-email=",
        "llmops.bootstrap.admin-password=",
})
class LlmopsEvalApplicationTests {

    @Test
    void contextLoads() {
    }

}
