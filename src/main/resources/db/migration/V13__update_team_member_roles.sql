ALTER TABLE team_members
    DROP CONSTRAINT chk_team_member_role;

-- Migrate existing LEAD rows to CONTRIBUTOR
UPDATE team_members SET role = 'CONTRIBUTOR' WHERE role = 'LEAD';

ALTER TABLE team_members
    ADD CONSTRAINT chk_team_member_role CHECK (role IN ('OWNER', 'CONTRIBUTOR', 'MEMBER'));
