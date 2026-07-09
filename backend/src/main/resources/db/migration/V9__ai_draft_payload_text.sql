-- 开发环境若已应用 jsonb 版 V8，将 payload 改为 TEXT 以便 MyBatis 直接读写

ALTER TABLE ai_draft
    ALTER COLUMN payload TYPE TEXT USING payload::text;
