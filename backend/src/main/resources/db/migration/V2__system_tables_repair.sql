-- 补救：若 V1 在 flyway 已记录但表未创建（开发环境历史不一致）

CREATE TABLE IF NOT EXISTS sys_user (
    id                  BIGSERIAL PRIMARY KEY,
    password_hash       VARCHAR(128) NOT NULL,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sys_setting (
    id            BIGSERIAL PRIMARY KEY,
    setting_key   VARCHAR(64) NOT NULL UNIQUE,
    setting_value TEXT,
    remark        VARCHAR(256),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL PRIMARY KEY,
    action      VARCHAR(64) NOT NULL,
    target_type VARCHAR(32),
    target_id   BIGINT,
    detail      JSONB,
    operator    VARCHAR(64) NOT NULL DEFAULT 'admin',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_target ON audit_log (target_type, target_id);
