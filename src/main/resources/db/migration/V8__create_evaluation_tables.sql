-- V8: Create Evaluation tables
-- Stores evaluation runs, results, and metric scores

-- Evaluation Metrics Configuration
CREATE TABLE evaluation_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE, -- NULL means global/system metric

    -- Metric identification
    code VARCHAR(50) NOT NULL,           -- BLEU, ROUGE_L, FAITHFULNESS, etc.
    display_name VARCHAR(100) NOT NULL,
    description TEXT,

    -- Metric classification
    category VARCHAR(30) NOT NULL,       -- TRADITIONAL_NLP, SEMANTIC, RAG_SPECIFIC, LLM_AS_JUDGE, PERFORMANCE

    -- Requirements
    requires_ground_truth BOOLEAN NOT NULL DEFAULT false,
    requires_context BOOLEAN NOT NULL DEFAULT false,
    requires_judge_llm BOOLEAN NOT NULL DEFAULT false,

    -- Configuration
    judge_prompt_template TEXT,          -- Custom prompt for LLM-as-Judge metrics
    default_weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    default_threshold DOUBLE PRECISION,

    -- Status
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_system BOOLEAN NOT NULL DEFAULT false, -- System-defined vs user-created

    -- Audit
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uk_evaluation_metrics_project_code UNIQUE (project_id, code)
);

-- Evaluation Runs
CREATE TABLE evaluation_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    dataset_id UUID NOT NULL REFERENCES datasets(id) ON DELETE RESTRICT,

    -- Run identification
    name VARCHAR(200) NOT NULL,
    run_number INTEGER NOT NULL DEFAULT 1,

    -- Configuration
    evaluation_mode VARCHAR(20) NOT NULL DEFAULT 'PRE_GENERATED', -- PRE_GENERATED, LIVE_GENERATION
    target_llm_endpoint_id UUID REFERENCES llm_endpoints(id),     -- For LIVE_GENERATION
    judge_llm_endpoint_id UUID REFERENCES llm_endpoints(id),      -- For LLM-as-Judge metrics

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, RUNNING, COMPLETED, FAILED, CANCELLED

    -- Progress
    total_test_cases INTEGER NOT NULL DEFAULT 0,
    completed_test_cases INTEGER NOT NULL DEFAULT 0,
    failed_test_cases INTEGER NOT NULL DEFAULT 0,

    -- Results summary
    overall_score DOUBLE PRECISION,
    metric_summaries JSONB,              -- Aggregated metric statistics

    -- Resource tracking
    total_tokens BIGINT DEFAULT 0,
    total_cost DECIMAL(10, 6) DEFAULT 0,

    -- Trigger info
    trigger_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL', -- MANUAL, CI_CD, SCHEDULED
    trigger_reference VARCHAR(255),       -- CI/CD job ID, schedule name, etc.

    -- Error handling
    error_message TEXT,

    -- Audit
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,

    -- Constraints
    CONSTRAINT uk_evaluation_runs_project_name_number UNIQUE (project_id, name, run_number)
);

-- Evaluation Results (per test case)
CREATE TABLE evaluation_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluation_run_id UUID NOT NULL REFERENCES evaluation_runs(id) ON DELETE CASCADE,
    test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE RESTRICT,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, COMPLETED, FAILED, SKIPPED

    -- Generated output (for LIVE_GENERATION mode)
    generated_output TEXT,

    -- Performance metrics
    llm_latency_ms BIGINT,
    judge_latency_ms BIGINT,
    processing_time_ms BIGINT,

    -- Token usage
    input_tokens INTEGER DEFAULT 0,
    output_tokens INTEGER DEFAULT 0,

    -- Error handling
    error_message TEXT,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uk_evaluation_results_run_testcase UNIQUE (evaluation_run_id, test_case_id)
);

-- Evaluation Scores (per metric per test case)
CREATE TABLE evaluation_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluation_result_id UUID NOT NULL REFERENCES evaluation_results(id) ON DELETE CASCADE,
    metric_id UUID NOT NULL REFERENCES evaluation_metrics(id) ON DELETE RESTRICT,

    -- Scores
    score DOUBLE PRECISION NOT NULL,      -- Normalized 0-1
    raw_score DOUBLE PRECISION NOT NULL,  -- Original value
    passed BOOLEAN,                        -- Pass/fail if threshold configured

    -- Details
    score_details JSONB,                   -- Metric-specific breakdown
    judge_reasoning TEXT,                  -- LLM judge explanation

    -- Error
    error_message TEXT,

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uk_evaluation_scores_result_metric UNIQUE (evaluation_result_id, metric_id)
);

-- Run Metric Configuration (which metrics to use in a run)
CREATE TABLE evaluation_run_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluation_run_id UUID NOT NULL REFERENCES evaluation_runs(id) ON DELETE CASCADE,
    metric_id UUID NOT NULL REFERENCES evaluation_metrics(id) ON DELETE RESTRICT,

    -- Override configuration
    weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    pass_threshold DOUBLE PRECISION,
    custom_prompt_template TEXT,

    -- Constraints
    CONSTRAINT uk_evaluation_run_metrics UNIQUE (evaluation_run_id, metric_id)
);

-- Indexes
CREATE INDEX idx_evaluation_metrics_project_id ON evaluation_metrics(project_id);
CREATE INDEX idx_evaluation_metrics_category ON evaluation_metrics(category);
CREATE INDEX idx_evaluation_metrics_code ON evaluation_metrics(code);

CREATE INDEX idx_evaluation_runs_project_id ON evaluation_runs(project_id);
CREATE INDEX idx_evaluation_runs_dataset_id ON evaluation_runs(dataset_id);
CREATE INDEX idx_evaluation_runs_status ON evaluation_runs(status);
CREATE INDEX idx_evaluation_runs_created_at ON evaluation_runs(created_at DESC);

CREATE INDEX idx_evaluation_results_run_id ON evaluation_results(evaluation_run_id);
CREATE INDEX idx_evaluation_results_test_case_id ON evaluation_results(test_case_id);
CREATE INDEX idx_evaluation_results_status ON evaluation_results(status);

CREATE INDEX idx_evaluation_scores_result_id ON evaluation_scores(evaluation_result_id);
CREATE INDEX idx_evaluation_scores_metric_id ON evaluation_scores(metric_id);

CREATE INDEX idx_evaluation_run_metrics_run_id ON evaluation_run_metrics(evaluation_run_id);

-- Comments
COMMENT ON TABLE evaluation_runs IS 'Individual evaluation run executions';
COMMENT ON TABLE evaluation_results IS 'Per-test-case evaluation results';
COMMENT ON TABLE evaluation_scores IS 'Individual metric scores for each test case result';
COMMENT ON TABLE evaluation_metrics IS 'Configured evaluation metrics (system and custom)';
COMMENT ON TABLE evaluation_run_metrics IS 'Which metrics are used in each evaluation run';
