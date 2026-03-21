-- V14: Create LLM Health Check tables

CREATE TABLE llm_health_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    endpoint_id UUID NOT NULL REFERENCES llm_endpoints(id) ON DELETE CASCADE,

    -- Health status
    status VARCHAR(20) NOT NULL,          -- UP, DOWN, DEGRADED, UNKNOWN
    latency_ms BIGINT,                    -- Response time in milliseconds
    http_status_code INTEGER,             -- HTTP response code
    error_message TEXT,                   -- Error detail if DOWN/DEGRADED

    -- Model metadata (captured at check time)
    model_name VARCHAR(100),
    provider_type VARCHAR(50),
    context_window INTEGER,               -- Max context window tokens
    max_output_tokens INTEGER,

    -- Check metadata
    check_type VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED, MANUAL
    checked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Constraint: index by endpoint + time for history queries
    CONSTRAINT chk_health_status CHECK (status IN ('UP', 'DOWN', 'DEGRADED', 'UNKNOWN')),
    CONSTRAINT chk_check_type CHECK (check_type IN ('SCHEDULED', 'MANUAL'))
);

-- Indexes for common queries
CREATE INDEX idx_health_records_endpoint_id ON llm_health_records(endpoint_id);
CREATE INDEX idx_health_records_endpoint_checked ON llm_health_records(endpoint_id, checked_at DESC);
CREATE INDEX idx_health_records_status ON llm_health_records(status);
CREATE INDEX idx_health_records_checked_at ON llm_health_records(checked_at DESC);

-- Store latest status per endpoint (for dashboard - avoid full table scan)
CREATE TABLE llm_endpoint_health_status (
    endpoint_id UUID PRIMARY KEY REFERENCES llm_endpoints(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    latency_ms BIGINT,
    last_checked_at TIMESTAMP WITH TIME ZONE,
    last_error_message TEXT,
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    uptime_percentage NUMERIC(5,2),       -- Last 24h uptime %
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

COMMENT ON TABLE llm_health_records IS 'Historical health check results per LLM endpoint';
COMMENT ON TABLE llm_endpoint_health_status IS 'Latest aggregated health status per endpoint (for fast dashboard reads)';
