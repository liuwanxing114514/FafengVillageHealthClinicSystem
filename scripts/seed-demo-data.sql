-- 发凤村卫生室演示数据（幂等：已存在则跳过）
-- 执行：scripts/seed-demo.sh 或 docker compose exec -T postgres psql ...

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM medicine WHERE name = '阿莫西林胶囊' LIMIT 1) THEN
        RAISE NOTICE '演示数据已存在，跳过。';
        RETURN;
    END IF;

    INSERT INTO medicine (name, generic_name, dosage_form, specification, base_unit, package_unit,
                          manufacturer, purchase_price, suggested_retail_price, stock_threshold, pinyin_abbr, status)
    VALUES
        ('阿莫西林胶囊', '阿莫西林', '胶囊', '0.25g*24粒', '粒', '盒', '示例制药', 12.50, 18.00, 50, 'AMXLJN', 'ACTIVE'),
        ('布洛芬片', '布洛芬', '片剂', '0.1g*100片', '片', '瓶', '示例制药', 8.00, 12.00, 100, 'BLFP', 'ACTIVE'),
        ('复方感冒灵颗粒', '复方感冒灵', '颗粒', '10g*9袋', '袋', '盒', '示例制药', 15.00, 22.00, 30, 'FFGMLKL', 'ACTIVE');

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
        ('李四', 'F', '1992-03-20', 33, TRUE, '13800002222', '发凤村二组', 'ACTIVE'),
        ('王五', 'M', '1978-11-08', 47, TRUE, '13800003333', '发凤村三组', 'ACTIVE'),
        ('赵六', 'F', '2001-04-12', 24, TRUE, '13800004444', '发凤村一组', 'ACTIVE'),
        ('陈七', 'M', '1965-09-25', 60, TRUE, '13800005555', '发凤村四组', 'ACTIVE');

    -- 脱敏样例病历（remark=演示数据，供 RAG 相似检索自测）
    INSERT INTO clinic_visit (patient_id, chief_complaint, present_illness, past_history, diagnosis, treatment, remark, status)
    SELECT p.id,
           '咽痛、发热 2 天',
           '自诉 2 天前受凉后出现咽痛，体温最高 38.5℃，干咳少痰。',
           '体健',
           '急性上呼吸道感染',
           '对症处理，嘱多饮水休息。',
           '演示数据',
           'ACTIVE'
    FROM patient p WHERE p.name = '张三';

    INSERT INTO clinic_visit (patient_id, chief_complaint, present_illness, diagnosis, treatment, remark, status)
    SELECT p.id,
           '咳嗽、流涕 3 天',
           '受凉后咳嗽，白色粘痰，鼻塞流涕，无高热。',
           '急性支气管炎',
           '止咳化痰，避免受凉。',
           '演示数据',
           'ACTIVE'
    FROM patient p WHERE p.name = '李四';

    INSERT INTO clinic_visit (patient_id, chief_complaint, present_illness, diagnosis, treatment, remark, status)
    SELECT p.id,
           '发热、咽痛 1 天',
           '体温 38.2℃，咽红，无呼吸困难。',
           '上呼吸道感染',
           '物理降温，口服退热药。',
           '演示数据',
           'ACTIVE'
    FROM patient p WHERE p.name = '赵六';

    INSERT INTO clinic_visit (patient_id, chief_complaint, present_illness, diagnosis, treatment, remark, status)
    SELECT p.id,
           '头痛、发热伴乏力',
           '近 3 日低热，头痛，全身酸痛，食欲差。',
           '感冒',
           '休息，多饮水，对症处理。',
           '演示数据',
           'ACTIVE'
    FROM patient p WHERE p.name = '王五';

    INSERT INTO clinic_visit (patient_id, chief_complaint, present_illness, diagnosis, treatment, remark, status)
    SELECT p.id,
           '腹痛、腹泻 2 天',
           '进食不洁后腹泻，每日 4-5 次稀便，无血便，轻度腹痛。',
           '急性胃肠炎',
           '补液，调节肠道菌群，清淡饮食。',
           '演示数据',
           'ACTIVE'
    FROM patient p WHERE p.name = '陈七';

    INSERT INTO clinic_visit (patient_id, chief_complaint, present_illness, diagnosis, treatment, remark, status)
    SELECT p.id,
           '上腹不适、恶心',
           '餐后腹胀，偶有反酸，无呕血黑便。',
           '慢性胃炎急性发作',
           '抑酸护胃，规律饮食。',
           '演示数据',
           'ACTIVE'
    FROM patient p WHERE p.name = '张三';

    INSERT INTO clinic_visit (patient_id, chief_complaint, present_illness, diagnosis, treatment, remark, status)
    SELECT p.id,
           '腰痛 1 周',
           '劳累后腰部酸痛，无下肢麻木，活动可。',
           '腰肌劳损',
           '休息，热敷，避免重体力劳动。',
           '演示数据',
           'ACTIVE'
    FROM patient p WHERE p.name = '王五';

    INSERT INTO clinic_visit (patient_id, chief_complaint, present_illness, diagnosis, treatment, remark, status)
    SELECT p.id,
           '膝关节疼痛',
           '上下楼梯时膝痛，无红肿热，晨僵不明显。',
           '膝关节骨性关节炎',
           '减少负重，局部理疗。',
           '演示数据',
           'ACTIVE'
    FROM patient p WHERE p.name = '陈七';

    INSERT INTO clinic_visit (patient_id, chief_complaint, present_illness, diagnosis, treatment, remark, status)
    SELECT p.id,
           '头晕、血压偏高',
           '自测血压 150/95mmHg，无胸痛，偶有头晕。',
           '高血压病',
           '低盐饮食，监测血压，必要时用药。',
           '演示数据',
           'ACTIVE'
    FROM patient p WHERE p.name = '李四';

    INSERT INTO clinic_visit (patient_id, chief_complaint, present_illness, diagnosis, treatment, remark, status)
    SELECT p.id,
           '皮肤瘙痒、红疹',
           '接触花粉后四肢出现红疹，瘙痒明显。',
           '过敏性皮炎',
           '避免接触过敏原，抗过敏治疗。',
           '演示数据',
           'ACTIVE'
    FROM patient p WHERE p.name = '赵六';

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

    RAISE NOTICE '演示数据导入完成（含 % 条样例病历）。', (SELECT COUNT(*) FROM clinic_visit WHERE remark = '演示数据');
END $$;
