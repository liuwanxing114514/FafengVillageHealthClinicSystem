-- v0.5 处方与明细

CREATE TABLE prescription (
    id                 BIGSERIAL PRIMARY KEY,
    patient_id         BIGINT       NOT NULL REFERENCES patient (id),
    visit_id           BIGINT       NOT NULL REFERENCES clinic_visit (id),
    prescription_date  DATE         NOT NULL DEFAULT CURRENT_DATE,
    diagnosis          TEXT,
    status             VARCHAR(16)  NOT NULL DEFAULT 'CONFIRMED',
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prescription_patient ON prescription (patient_id);
CREATE INDEX idx_prescription_visit ON prescription (visit_id);
CREATE INDEX idx_prescription_date ON prescription (prescription_date DESC);

CREATE TABLE prescription_item (
    id               BIGSERIAL PRIMARY KEY,
    prescription_id  BIGINT         NOT NULL REFERENCES prescription (id) ON DELETE CASCADE,
    medicine_id      BIGINT         NOT NULL,
    dosage_form      VARCHAR(32),
    medicine_name    VARCHAR(128)   NOT NULL,
    specification    VARCHAR(64),
    quantity         NUMERIC(14, 3) NOT NULL,
    unit             VARCHAR(16)    NOT NULL,
    usage            VARCHAR(256),
    sort_order       INT            NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prescription_item_prescription ON prescription_item (prescription_id);
