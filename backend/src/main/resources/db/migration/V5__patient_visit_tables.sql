-- v0.4 患者资料与门诊病历

CREATE TABLE patient (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(64)  NOT NULL,
    gender       VARCHAR(8)   NOT NULL DEFAULT 'UNKNOWN',
    id_card      VARCHAR(18),
    birth_date   DATE,
    age          INT,
    age_manual   BOOLEAN      NOT NULL DEFAULT TRUE,
    phone        VARCHAR(20)  NOT NULL DEFAULT '',
    address      VARCHAR(256) NOT NULL DEFAULT '',
    remark       TEXT,
    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_patient_id_card ON patient (id_card) WHERE id_card IS NOT NULL AND id_card <> '';
CREATE INDEX idx_patient_name ON patient (name);
CREATE INDEX idx_patient_phone ON patient (phone);

CREATE TABLE clinic_visit (
    id                BIGSERIAL PRIMARY KEY,
    patient_id        BIGINT       NOT NULL REFERENCES patient (id),
    visit_time        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    chief_complaint   TEXT,
    present_illness   TEXT,
    past_history      TEXT,
    temperature       NUMERIC(4, 1),
    blood_pressure    VARCHAR(16),
    spo2              NUMERIC(5, 2),
    etco2             NUMERIC(5, 2),
    heart_rate        INT,
    pulse             VARCHAR(64),
    allergy_history   TEXT,
    diagnosis         TEXT,
    treatment         TEXT,
    remark            TEXT,
    status            VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clinic_visit_patient_time ON clinic_visit (patient_id, visit_time DESC);
