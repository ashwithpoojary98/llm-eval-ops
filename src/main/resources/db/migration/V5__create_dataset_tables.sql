-- Datasets
CREATE TABLE datasets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT uk_datasets_project_name UNIQUE (project_id, name)
);

-- Dataset-TestCase join table (Many-to-Many)
CREATE TABLE dataset_test_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dataset_id UUID NOT NULL REFERENCES datasets(id) ON DELETE CASCADE,
    test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    added_by UUID REFERENCES users(id),
    added_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT uk_dataset_test_cases UNIQUE (dataset_id, test_case_id)
);

-- Indexes
CREATE INDEX idx_datasets_project_id ON datasets(project_id);
CREATE INDEX idx_datasets_project_active ON datasets(project_id, is_active);
CREATE INDEX idx_datasets_created_by ON datasets(created_by);
CREATE INDEX idx_dataset_test_cases_dataset_id ON dataset_test_cases(dataset_id);
CREATE INDEX idx_dataset_test_cases_test_case_id ON dataset_test_cases(test_case_id);
