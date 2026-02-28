-- V10: Ensure NOT NULL constraints on timestamp and token columns
-- This migration is idempotent - safe to run even if constraints already exist

-- Set default values for any NULL values first (idempotent)
UPDATE evaluation_results SET created_at = NOW() WHERE created_at IS NULL;
UPDATE evaluation_results SET input_tokens = 0 WHERE input_tokens IS NULL;
UPDATE evaluation_results SET output_tokens = 0 WHERE output_tokens IS NULL;
UPDATE evaluation_scores SET created_at = NOW() WHERE created_at IS NULL;
UPDATE evaluation_runs SET created_at = NOW() WHERE created_at IS NULL;

-- Add NOT NULL constraints (PostgreSQL allows SET NOT NULL on already NOT NULL columns)
DO $$
BEGIN
    -- evaluation_results
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'evaluation_results' AND column_name = 'created_at' AND is_nullable = 'YES') THEN
        ALTER TABLE evaluation_results ALTER COLUMN created_at SET NOT NULL;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'evaluation_results' AND column_name = 'input_tokens' AND is_nullable = 'YES') THEN
        ALTER TABLE evaluation_results ALTER COLUMN input_tokens SET NOT NULL;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'evaluation_results' AND column_name = 'output_tokens' AND is_nullable = 'YES') THEN
        ALTER TABLE evaluation_results ALTER COLUMN output_tokens SET NOT NULL;
    END IF;

    -- evaluation_scores
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'evaluation_scores' AND column_name = 'created_at' AND is_nullable = 'YES') THEN
        ALTER TABLE evaluation_scores ALTER COLUMN created_at SET NOT NULL;
    END IF;

    -- evaluation_runs
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'evaluation_runs' AND column_name = 'created_at' AND is_nullable = 'YES') THEN
        ALTER TABLE evaluation_runs ALTER COLUMN created_at SET NOT NULL;
    END IF;
END $$;
