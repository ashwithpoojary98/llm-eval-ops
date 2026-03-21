package io.github.ashwithpoojary98.llmops_eval.health.scheduler;

import io.github.ashwithpoojary98.llmops_eval.health.service.LlmHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class HealthCheckScheduler {

    private final LlmHealthService llmHealthService;

    /**
     * Run health checks every 5 minutes for all active endpoints.
     * Configurable via llmops.health.check-interval-ms (default 5 minutes).
     */
    @Scheduled(fixedDelayString = "${llmops.health.check-interval-ms:300000}")
    public void runHealthChecks() {
        log.debug("Starting scheduled health checks at {}", Instant.now());
        try {
            llmHealthService.runScheduledChecksForAllEndpoints();
        } catch (Exception e) {
            log.error("Scheduled health check run failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Purge health records older than 30 days to keep the table lean.
     * Runs daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void purgeOldHealthRecords() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        log.info("Purging health records older than {}", cutoff);
        llmHealthService.purgeRecordsBefore(cutoff);
    }
}
