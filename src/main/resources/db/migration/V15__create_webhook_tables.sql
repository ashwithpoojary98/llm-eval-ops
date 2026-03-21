-- V15: Create Webhook tables

CREATE TABLE webhooks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE, -- NULL = org-wide

    name VARCHAR(100) NOT NULL,
    url TEXT NOT NULL,
    secret_hash TEXT,                     -- HMAC-SHA256 signing secret (hashed for storage)
    secret_encrypted TEXT,                -- Encrypted signing secret (for delivery)

    -- Which events to subscribe to (stored as JSONB array)
    events JSONB NOT NULL DEFAULT '[]',

    is_active BOOLEAN NOT NULL DEFAULT true,
    failure_count INTEGER NOT NULL DEFAULT 0,
    last_triggered_at TIMESTAMP WITH TIME ZONE,

    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE webhook_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    webhook_id UUID NOT NULL REFERENCES webhooks(id) ON DELETE CASCADE,

    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,               -- Full event payload sent
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED, RETRYING

    -- Response tracking
    response_status_code INTEGER,
    response_body TEXT,
    error_message TEXT,

    -- Retry tracking
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP WITH TIME ZONE,

    triggered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    delivered_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_delivery_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'RETRYING'))
);

-- Indexes
CREATE INDEX idx_webhooks_org_id ON webhooks(organization_id);
CREATE INDEX idx_webhooks_project_id ON webhooks(project_id);
CREATE INDEX idx_webhooks_active ON webhooks(is_active);
CREATE INDEX idx_webhook_deliveries_webhook_id ON webhook_deliveries(webhook_id);
CREATE INDEX idx_webhook_deliveries_status ON webhook_deliveries(status);
CREATE INDEX idx_webhook_deliveries_triggered ON webhook_deliveries(triggered_at DESC);
CREATE INDEX idx_webhook_deliveries_next_retry ON webhook_deliveries(next_retry_at) WHERE status = 'RETRYING';

COMMENT ON TABLE webhooks IS 'Registered webhook endpoints for event delivery';
COMMENT ON TABLE webhook_deliveries IS 'Delivery log for all webhook events with retry tracking';
