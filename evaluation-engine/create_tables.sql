-- Evaluation Engine Tables
-- Run this SQL in PostgreSQL to create the required tables

-- Drop existing tables if they exist (uncomment if needed)
-- DROP TABLE IF EXISTS metric_scores CASCADE;
-- DROP TABLE IF EXISTS evaluation_results CASCADE;
-- DROP TABLE IF EXISTS evaluation_tasks CASCADE;

-- Table: evaluation_tasks
CREATE TABLE IF NOT EXISTS evaluation_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluation_run_id VARCHAR(36) UNIQUE NOT NULL,
    project_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) DEFAULT 'QUEUED',
    evaluation_mode VARCHAR(20),
    total_test_cases INTEGER DEFAULT 0,
    completed_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    overall_score FLOAT,
    metric_summaries JSONB,
    total_tokens INTEGER DEFAULT 0,
    total_cost FLOAT DEFAULT 0.0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    callback_url VARCHAR(500)
);

-- Table: evaluation_results
CREATE TABLE IF NOT EXISTS evaluation_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID REFERENCES evaluation_tasks(id) ON DELETE CASCADE,
    test_case_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    generated_output TEXT,
    llm_latency_ms INTEGER,
    processing_time_ms INTEGER,
    input_tokens INTEGER DEFAULT 0,
    output_tokens INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Table: metric_scores
CREATE TABLE IF NOT EXISTS metric_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id UUID REFERENCES evaluation_results(id) ON DELETE CASCADE,
    metric_code VARCHAR(50) NOT NULL,
    score FLOAT NOT NULL,
    raw_score FLOAT NOT NULL,
    passed BOOLEAN,
    details JSONB,
    reasoning TEXT,
    error TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS ix_evaluation_tasks_evaluation_run_id ON evaluation_tasks(evaluation_run_id);
CREATE INDEX IF NOT EXISTS ix_evaluation_tasks_project_id ON evaluation_tasks(project_id);
CREATE INDEX IF NOT EXISTS ix_evaluation_results_task_id ON evaluation_results(task_id);
CREATE INDEX IF NOT EXISTS ix_evaluation_results_test_case_id ON evaluation_results(test_case_id);
CREATE INDEX IF NOT EXISTS ix_metric_scores_result_id ON metric_scores(result_id);

-- Verify tables created
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public'
AND table_name IN ('evaluation_tasks', 'evaluation_results', 'metric_scores');
