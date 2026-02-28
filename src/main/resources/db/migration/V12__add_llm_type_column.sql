-- V12: Add llm_type column to distinguish Client LLM from Judge LLM

-- Add the llm_type column with default value 'CLIENT'
ALTER TABLE llm_endpoints
ADD COLUMN IF NOT EXISTS llm_type VARCHAR(20) NOT NULL DEFAULT 'CLIENT';

-- Add index for filtering by type
CREATE INDEX IF NOT EXISTS idx_llm_endpoints_llm_type ON llm_endpoints(llm_type);

-- Add comment
COMMENT ON COLUMN llm_endpoints.llm_type IS 'Purpose of the LLM: CLIENT (being tested) or JUDGE (evaluates responses)';
