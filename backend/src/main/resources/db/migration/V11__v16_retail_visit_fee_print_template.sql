-- v1.6 建议零售价、就诊收费、处方打印模板默认配置

ALTER TABLE medicine
    ADD COLUMN IF NOT EXISTS suggested_retail_price NUMERIC(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE clinic_visit
    ADD COLUMN IF NOT EXISTS amount_due  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS amount_paid NUMERIC(12, 2) NOT NULL DEFAULT 0;

INSERT INTO sys_setting (setting_key, setting_value, remark)
VALUES (
    'prescription_print_active_template',
    'default-a4',
    '当前处方打印模板：default-a4 | preprinted-fafeng'
)
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO sys_setting (setting_key, setting_value, remark)
VALUES (
    'prescription_print_template_config',
    '{"templates":{"default-a4":{"type":"full-page","title":"发凤村卫生室处方签"},"preprinted-fafeng":{"type":"overlay","page":{"widthMm":210,"heightMm":297},"staticValues":{"department":"全科"},"fields":[{"key":"patientName","topMm":28,"leftMm":18,"fontSizePt":12},{"key":"gender","topMm":28,"leftMm":52,"fontSizePt":12},{"key":"age","topMm":28,"leftMm":72,"fontSizePt":12},{"key":"visitRecordNo","topMm":36,"leftMm":32,"fontSizePt":12},{"key":"department","topMm":36,"leftMm":72,"fontSizePt":12},{"key":"address","topMm":44,"leftMm":18,"fontSizePt":12},{"key":"phone","topMm":44,"leftMm":120,"fontSizePt":12},{"key":"diagnosis","topMm":52,"leftMm":38,"fontSizePt":12},{"key":"dateYear","topMm":60,"leftMm":32,"fontSizePt":12},{"key":"dateMonth","topMm":60,"leftMm":52,"fontSizePt":12},{"key":"dateDay","topMm":60,"leftMm":68,"fontSizePt":12},{"key":"doctorSignature","topMm":248,"leftMm":130,"fontSizePt":12}],"itemsArea":{"topMm":72,"leftMm":18,"lineHeightMm":6,"fontSizePt":12,"rpLabel":"Rp"}}}',
    '处方打印模板坐标配置 JSON'
)
ON CONFLICT (setting_key) DO NOTHING;
