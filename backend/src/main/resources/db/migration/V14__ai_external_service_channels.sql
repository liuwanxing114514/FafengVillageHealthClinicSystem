-- v2.5 AI 外部服务开关 + Chat/Embedding 多通道配置

CREATE TABLE external_service (
    service_code  VARCHAR(32) PRIMARY KEY,
    enabled       BOOLEAN NOT NULL DEFAULT FALSE,
    endpoint_url  VARCHAR(512) NOT NULL DEFAULT '',
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ai_chat_channel (
    id            BIGSERIAL PRIMARY KEY,
    channel_id    VARCHAR(64) NOT NULL UNIQUE,
    display_name  VARCHAR(128) NOT NULL,
    priority      INT NOT NULL DEFAULT 1,
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    base_url      VARCHAR(512) NOT NULL,
    api_key_enc   TEXT NOT NULL DEFAULT '',
    model         VARCHAR(128) NOT NULL,
    temperature   NUMERIC(3, 2) NOT NULL DEFAULT 0.2,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_chat_channel_priority ON ai_chat_channel (priority ASC, id ASC);
CREATE INDEX idx_ai_chat_channel_enabled ON ai_chat_channel (enabled);

CREATE TABLE ai_embedding_channel (
    id            BIGSERIAL PRIMARY KEY,
    channel_id    VARCHAR(64) NOT NULL UNIQUE,
    display_name  VARCHAR(128) NOT NULL,
    priority      INT NOT NULL DEFAULT 1,
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    base_url      VARCHAR(512) NOT NULL,
    api_key_enc   TEXT NOT NULL DEFAULT '',
    model         VARCHAR(128) NOT NULL,
    dimensions    INT NOT NULL DEFAULT 1024,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_embedding_channel_priority ON ai_embedding_channel (priority ASC, id ASC);
CREATE INDEX idx_ai_embedding_channel_enabled ON ai_embedding_channel (enabled);
