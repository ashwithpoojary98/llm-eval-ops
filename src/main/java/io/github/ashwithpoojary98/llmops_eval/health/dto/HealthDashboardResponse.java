package io.github.ashwithpoojary98.llmops_eval.health.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HealthDashboardResponse {
    private int totalEndpoints;
    private int upCount;
    private int downCount;
    private int degradedCount;
    private int unknownCount;
    private List<EndpointHealthSummary> endpoints;
}
