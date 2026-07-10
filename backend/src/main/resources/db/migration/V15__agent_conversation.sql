-- Agent 分会话与消息持久化

CREATE TABLE agent_conversation (
    id              VARCHAR(64)  PRIMARY KEY,
    title           VARCHAR(120) NOT NULL DEFAULT '新对话',
    message_count   INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_conversation_updated ON agent_conversation (updated_at DESC);

CREATE TABLE agent_message (
    id                   BIGSERIAL    PRIMARY KEY,
    conversation_id      VARCHAR(64)  NOT NULL REFERENCES agent_conversation (id) ON DELETE CASCADE,
    role                 VARCHAR(16)  NOT NULL,
    content              TEXT         NOT NULL,
    tool_calls_json      TEXT,
    references_json      TEXT,
    pending_actions_json TEXT,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_message_conversation ON agent_message (conversation_id, id ASC);
