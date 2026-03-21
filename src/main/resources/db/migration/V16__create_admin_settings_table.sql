-- V16: Admin settings table (per-organization configurable settings)

CREATE TABLE admin_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,

    setting_key VARCHAR(100) NOT NULL,       -- e.g. 'email.smtp.host'
    setting_value TEXT,                      -- plain value
    encrypted_value TEXT,                    -- encrypted (for passwords/secrets)
    is_encrypted BOOLEAN NOT NULL DEFAULT false,

    description VARCHAR(255),
    updated_by UUID REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT uk_admin_settings_org_key UNIQUE (organization_id, setting_key)
);

CREATE INDEX idx_admin_settings_org_id ON admin_settings(organization_id);
CREATE INDEX idx_admin_settings_key ON admin_settings(setting_key);

COMMENT ON TABLE admin_settings IS 'Per-organization configurable platform settings (email, notifications, etc.)';
COMMENT ON COLUMN admin_settings.setting_key IS 'Dot-notation key, e.g. email.smtp.host, email.smtp.password';
COMMENT ON COLUMN admin_settings.is_encrypted IS 'If true, read from encrypted_value instead of setting_value';
