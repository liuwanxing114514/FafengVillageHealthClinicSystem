-- audit_log.detail 在 MyBatis 中按字符串写入，改为 TEXT 避免 JSONB 类型错误

ALTER TABLE audit_log
    ALTER COLUMN detail TYPE TEXT USING detail::text;
