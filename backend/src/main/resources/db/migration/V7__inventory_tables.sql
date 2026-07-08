-- v0.6 库存批次与流水

CREATE TABLE inventory_batch (
    id              BIGSERIAL PRIMARY KEY,
    medicine_id     BIGINT         NOT NULL,
    batch_no        VARCHAR(64)    NOT NULL,
    expiry_date     DATE,
    quantity        NUMERIC(14, 3) NOT NULL DEFAULT 0,
    purchase_price  NUMERIC(12, 2),
    supplier        VARCHAR(128)   NOT NULL DEFAULT '',
    status          VARCHAR(16)    NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_inventory_batch_medicine_batch ON inventory_batch (medicine_id, batch_no);
CREATE INDEX idx_inventory_batch_medicine_expiry ON inventory_batch (medicine_id, expiry_date);
CREATE INDEX idx_inventory_batch_medicine_status ON inventory_batch (medicine_id, status);

CREATE TABLE inventory_flow (
    id               BIGSERIAL PRIMARY KEY,
    medicine_id      BIGINT         NOT NULL,
    batch_id         BIGINT         REFERENCES inventory_batch (id),
    flow_type        VARCHAR(16)    NOT NULL,
    quantity_change  NUMERIC(14, 3) NOT NULL,
    quantity_before  NUMERIC(14, 3) NOT NULL,
    quantity_after   NUMERIC(14, 3) NOT NULL,
    unit             VARCHAR(16)    NOT NULL,
    patient_id       BIGINT,
    prescription_id  BIGINT,
    reason           VARCHAR(256),
    remark           TEXT,
    operator         VARCHAR(64)    NOT NULL DEFAULT 'admin',
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inventory_flow_medicine_time ON inventory_flow (medicine_id, created_at DESC);
CREATE INDEX idx_inventory_flow_prescription ON inventory_flow (prescription_id);
CREATE INDEX idx_inventory_flow_batch ON inventory_flow (batch_id);
