-- Password reset tokens table
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for token lookup
CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);

-- Index for user lookup
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);

-- Index for cleanup of expired tokens
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);
