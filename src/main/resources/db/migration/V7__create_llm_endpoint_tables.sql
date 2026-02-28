-- V7: Create LLM Endpoint tables
-- Stores configuration for LLM providers (OpenAI, Anthropic, Azure, Custom)

CREATE TABLE llm_endpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,

    -- Provider configuration
    provider_type VARCHAR(50) NOT NULL, -- OPENAI, ANTHROPIC, AZURE_OPENAI, AWS_BEDROCK, GOOGLE_VERTEX, CUSTOM
    model_name VARCHAR(100) NOT NULL,
    api_url VARCHAR(500),              -- Custom API URL (required for AZURE, CUSTOM)

    -- Authentication
    auth_type VARCHAR(20) NOT NULL DEFAULT 'API_KEY', -- API_KEY, BEARER, BASIC, NONE
    encrypted_api_key TEXT,             -- Encrypted API key

    -- Model configuration (stored as JSONB)
    model_config JSONB DEFAULT '{}',    -- temperature, max_tokens, etc.

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_default BOOLEAN NOT NULL DEFAULT false,

    -- Audit
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uk_llm_endpoints_project_name UNIQUE (project_id, name)
);

-- Indexes
CREATE INDEX idx_llm_endpoints_project_id ON llm_endpoints(project_id);
CREATE INDEX idx_llm_endpoints_project_active ON llm_endpoints(project_id, is_active);
CREATE INDEX idx_llm_endpoints_provider_type ON llm_endpoints(provider_type);

-- Comments
COMMENT ON TABLE llm_endpoints IS 'Configuration for LLM API endpoints used for evaluation';
COMMENT ON COLUMN llm_endpoints.provider_type IS 'LLM provider: OPENAI, ANTHROPIC, AZURE_OPENAI, AWS_BEDROCK, GOOGLE_VERTEX, CUSTOM';
COMMENT ON COLUMN llm_endpoints.encrypted_api_key IS 'API key encrypted using application encryption key';
COMMENT ON COLUMN llm_endpoints.model_config IS 'JSON configuration: temperature, max_tokens, top_p, etc.';
