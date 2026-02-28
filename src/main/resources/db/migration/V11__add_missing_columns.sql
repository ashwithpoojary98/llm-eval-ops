-- V11: Add missing columns to evaluation tables
-- Adds columns that may be missing if schema was partially applied

-- Add judge_latency_ms if missing (using INTEGER to match existing column types)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'evaluation_results' AND column_name = 'judge_latency_ms') THEN
        ALTER TABLE evaluation_results ADD COLUMN judge_latency_ms INTEGER;
    END IF;
END $$;
