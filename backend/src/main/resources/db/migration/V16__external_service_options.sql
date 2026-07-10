-- OCR 混合模式：external_service 扩展 options_json（mode / visionModel）

ALTER TABLE external_service
    ADD COLUMN IF NOT EXISTS options_json JSONB NOT NULL DEFAULT '{}';
