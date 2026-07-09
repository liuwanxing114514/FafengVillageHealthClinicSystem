-- v0.9 AI 草稿表

CREATE TABLE ai_draft (
    id          BIGSERIAL PRIMARY KEY,
    draft_type  VARCHAR(16)  NOT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    payload     JSONB        NOT NULL DEFAULT '{}',
    source      VARCHAR(32)  NOT NULL DEFAULT 'noop',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_draft_type_status ON ai_draft (draft_type, status, created_at DESC);
