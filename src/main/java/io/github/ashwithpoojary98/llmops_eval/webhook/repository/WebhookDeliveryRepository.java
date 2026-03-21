package io.github.ashwithpoojary98.llmops_eval.webhook.repository;

import io.github.ashwithpoojary98.llmops_eval.webhook.entity.DeliveryStatus;
import io.github.ashwithpoojary98.llmops_eval.webhook.entity.WebhookDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    Page<WebhookDelivery> findByWebhookIdOrderByTriggeredAtDesc(UUID webhookId, Pageable pageable);

    @Query("SELECT d FROM WebhookDelivery d WHERE d.status = 'RETRYING' AND d.nextRetryAt <= :now")
    List<WebhookDelivery> findPendingRetries(@Param("now") Instant now);

    long countByWebhookIdAndStatus(UUID webhookId, DeliveryStatus status);
}
