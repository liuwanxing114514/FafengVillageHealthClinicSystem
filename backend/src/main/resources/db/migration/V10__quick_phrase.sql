-- v1.2 快捷语候选

CREATE TABLE quick_phrase (
    id           BIGSERIAL PRIMARY KEY,
    field_key    VARCHAR(32)  NOT NULL,
    content      TEXT         NOT NULL,
    source       VARCHAR(16)  NOT NULL DEFAULT 'MANUAL',
    use_count    INT          NOT NULL DEFAULT 0,
    last_used_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_quick_phrase_source CHECK (source IN ('MANUAL', 'HISTORY'))
);

CREATE UNIQUE INDEX uk_quick_phrase_field_content ON quick_phrase (field_key, content);
CREATE INDEX idx_quick_phrase_field_rank ON quick_phrase (field_key, use_count DESC, last_used_at DESC NULLS LAST);

INSERT INTO sys_setting (setting_key, setting_value, remark)
VALUES
    ('quick_phrase_cleanup_days', '180', '低频快捷语清理：超过此天数未使用且来源为历史统计'),
    ('quick_phrase_cleanup_min_count', '1', '低频快捷语清理：使用次数低于等于此值')
ON CONFLICT (setting_key) DO NOTHING;
