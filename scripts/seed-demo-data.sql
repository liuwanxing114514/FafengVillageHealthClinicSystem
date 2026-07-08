-- 发凤村卫生室演示数据（幂等：已存在则跳过）
-- 执行：scripts/seed-demo.ps1 或 docker compose exec -T postgres psql ...

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM medicine WHERE name = '阿莫西林胶囊' LIMIT 1) THEN
        RAISE NOTICE '演示数据已存在，跳过。';
        RETURN;
    END IF;

    INSERT INTO medicine (name, generic_name, dosage_form, specification, base_unit, package_unit,
                          manufacturer, purchase_price, stock_threshold, pinyin_abbr, status)
    VALUES
        ('阿莫西林胶囊', '阿莫西林', '胶囊', '0.25g*24粒', '粒', '盒', '示例制药', 12.50, 50, 'AMXLJN', 'ACTIVE'),
        ('布洛芬片', '布洛芬', '片剂', '0.1g*100片', '片', '瓶', '示例制药', 8.00, 100, 'BLFP', 'ACTIVE'),
        ('复方感冒灵颗粒', '复方感冒灵', '颗粒', '10g*9袋', '袋', '盒', '示例制药', 15.00, 30, 'FFGMLKL', 'ACTIVE');

    INSERT INTO medicine_unit_conversion (medicine_id, from_unit, to_unit, factor)
    SELECT id, '盒', '粒', 24 FROM medicine WHERE name = '阿莫西林胶囊';

    INSERT INTO medicine_unit_conversion (medicine_id, from_unit, to_unit, factor)
    SELECT id, '盒', '袋', 9 FROM medicine WHERE name = '复方感冒灵颗粒';

    INSERT INTO medicine_barcode (medicine_id, barcode, remark)
    SELECT id, '6901234567890', '演示条码' FROM medicine WHERE name = '阿莫西林胶囊';

    INSERT INTO medicine_barcode (medicine_id, barcode, remark)
    SELECT id, '6901234567891', '演示条码' FROM medicine WHERE name = '布洛芬片';

    INSERT INTO patient (name, gender, birth_date, age, age_manual, phone, address, status)
    VALUES
        ('张三', 'M', '1985-06-15', 40, TRUE, '13800001111', '发凤村一组', 'ACTIVE'),
        ('李四', 'F', '1992-03-20', 33, TRUE, '13800002222', '发凤村二组', 'ACTIVE');

    INSERT INTO clinic_visit (patient_id, chief_complaint, present_illness, diagnosis, treatment, status)
    SELECT p.id,
           '咽痛、发热 2 天',
           '自诉 2 天前受凉后出现咽痛，体温最高 38.5℃，无咳嗽。',
           '急性上呼吸道感染',
           '对症处理，嘱多饮水休息。',
           'ACTIVE'
    FROM patient p WHERE p.name = '张三';

    INSERT INTO inventory_batch (medicine_id, batch_no, expiry_date, quantity, purchase_price, supplier, status)
    SELECT m.id, 'DEMO-AMX-001', CURRENT_DATE + INTERVAL '60 days', 120, 12.50, '县医药公司', 'ACTIVE'
    FROM medicine m WHERE m.name = '阿莫西林胶囊';

    INSERT INTO inventory_batch (medicine_id, batch_no, expiry_date, quantity, purchase_price, supplier, status)
    SELECT m.id, 'DEMO-IBU-001', CURRENT_DATE + INTERVAL '400 days', 20, 8.00, '县医药公司', 'ACTIVE'
    FROM medicine m WHERE m.name = '布洛芬片';

    INSERT INTO inventory_batch (medicine_id, batch_no, expiry_date, quantity, purchase_price, supplier, status)
    SELECT m.id, 'DEMO-COLD-001', CURRENT_DATE + INTERVAL '45 days', 18, 15.00, '县医药公司', 'ACTIVE'
    FROM medicine m WHERE m.name = '复方感冒灵颗粒';

    INSERT INTO inventory_flow (medicine_id, batch_id, flow_type, quantity_change, quantity_before, quantity_after, unit, reason, operator)
    SELECT b.medicine_id, b.id, 'INBOUND', b.quantity, 0, b.quantity, m.base_unit, '演示数据初始入库', 'system'
    FROM inventory_batch b
    JOIN medicine m ON m.id = b.medicine_id
    WHERE b.batch_no LIKE 'DEMO-%';

    RAISE NOTICE '演示数据导入完成。';
END $$;
