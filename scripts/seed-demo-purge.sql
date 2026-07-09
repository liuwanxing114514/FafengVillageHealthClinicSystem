-- 删除演示数据（供 seed-demo-refresh.sh 使用；勿在生产真实数据环境误用）
-- 仅删除 batch_no DEMO-*、演示药品名、演示患者及关联病历

DO $$
DECLARE
    demo_medicine_ids BIGINT[];
    demo_patient_ids BIGINT[];
    demo_visit_ids BIGINT[];
    demo_batch_ids BIGINT[];
BEGIN
    SELECT ARRAY_AGG(id) INTO demo_medicine_ids FROM medicine
    WHERE name IN ('阿莫西林胶囊', '布洛芬片', '复方感冒灵颗粒');

    IF demo_medicine_ids IS NULL THEN
        RAISE NOTICE '无演示药品，跳过清除。';
        RETURN;
    END IF;

    SELECT ARRAY_AGG(id) INTO demo_patient_ids FROM patient
    WHERE name IN ('张三', '李四', '王五', '赵六', '陈七')
       OR address LIKE '发凤村%组';

    SELECT ARRAY_AGG(id) INTO demo_visit_ids FROM clinic_visit
    WHERE remark = '演示数据'
       OR (demo_patient_ids IS NOT NULL AND patient_id = ANY(demo_patient_ids));

    SELECT ARRAY_AGG(id) INTO demo_batch_ids FROM inventory_batch
    WHERE batch_no LIKE 'DEMO-%';

    IF demo_visit_ids IS NOT NULL THEN
        DELETE FROM visit_embedding WHERE visit_id = ANY(demo_visit_ids);
        DELETE FROM clinic_visit WHERE id = ANY(demo_visit_ids);
    END IF;

    IF demo_batch_ids IS NOT NULL THEN
        DELETE FROM inventory_flow WHERE batch_id = ANY(demo_batch_ids);
        DELETE FROM inventory_batch WHERE id = ANY(demo_batch_ids);
    END IF;

    IF demo_patient_ids IS NOT NULL THEN
        DELETE FROM patient WHERE id = ANY(demo_patient_ids);
    END IF;

    DELETE FROM medicine_barcode WHERE medicine_id = ANY(demo_medicine_ids);
    DELETE FROM medicine_unit_conversion WHERE medicine_id = ANY(demo_medicine_ids);
    DELETE FROM medicine WHERE id = ANY(demo_medicine_ids);

    RAISE NOTICE '演示数据已清除。';
END $$;
