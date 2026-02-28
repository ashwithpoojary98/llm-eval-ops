-- Projects
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_projects_name UNIQUE (name),
    CONSTRAINT chk_project_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

-- Project Access Levels
-- ADMIN: Full control over project
-- WRITE: Can create/edit datasets, endpoints, run evaluations
-- READ: Can only view

-- Project Teams (Team assignment to projects)
CREATE TABLE project_teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    access_level VARCHAR(20) NOT NULL DEFAULT 'READ',
    assigned_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_project_teams UNIQUE (project_id, team_id),
    CONSTRAINT chk_access_level CHECK (access_level IN ('ADMIN', 'WRITE', 'READ'))
);

-- Project Members (Direct user assignment to projects)
CREATE TABLE project_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    access_level VARCHAR(20) NOT NULL DEFAULT 'READ',
    assigned_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_project_members UNIQUE (project_id, user_id),
    CONSTRAINT chk_member_access_level CHECK (access_level IN ('ADMIN', 'WRITE', 'READ'))
);

-- Indexes
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_projects_created_by ON projects(created_by);
CREATE INDEX idx_project_teams_project_id ON project_teams(project_id);
CREATE INDEX idx_project_teams_team_id ON project_teams(team_id);
CREATE INDEX idx_project_members_project_id ON project_members(project_id);
CREATE INDEX idx_project_members_user_id ON project_members(user_id);
