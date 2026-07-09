-- v2.0 Agent 工具执行日志

CREATE TABLE agent_execution_log (
    id              BIGSERIAL PRIMARY KEY,
    session_id      VARCHAR(64)  NOT NULL,
    tool_name       VARCHAR(64)  NOT NULL,
    args_summary    TEXT,
    result_summary  TEXT,
    duration_ms     INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_execution_log_session ON agent_execution_log (session_id, created_at DESC);
CREATE INDEX idx_agent_execution_log_created ON agent_execution_log (created_at DESC);
