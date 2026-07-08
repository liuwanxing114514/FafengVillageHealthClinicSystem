-- v0.3 药品资料、单位换算、条码

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE medicine (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    generic_name    VARCHAR(128) NOT NULL DEFAULT '',
    dosage_form     VARCHAR(32)  NOT NULL DEFAULT '',
    specification   VARCHAR(64)  NOT NULL DEFAULT '',
    base_unit       VARCHAR(16)  NOT NULL,
    package_unit    VARCHAR(16)  NOT NULL DEFAULT '',
    manufacturer    VARCHAR(128) NOT NULL DEFAULT '',
    purchase_price  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    stock_threshold NUMERIC(14, 3) NOT NULL DEFAULT 5,
    pinyin_abbr     VARCHAR(64)  NOT NULL DEFAULT '',
    remark          TEXT,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_medicine_name ON medicine (name);
CREATE INDEX idx_medicine_pinyin_abbr ON medicine (pinyin_abbr);
CREATE INDEX idx_medicine_status ON medicine (status);
CREATE INDEX idx_medicine_name_trgm ON medicine USING GIN (name gin_trgm_ops);

CREATE TABLE medicine_unit_conversion (
    id           BIGSERIAL PRIMARY KEY,
    medicine_id  BIGINT       NOT NULL REFERENCES medicine (id),
    from_unit    VARCHAR(16)  NOT NULL,
    to_unit      VARCHAR(16)  NOT NULL,
    factor       INT          NOT NULL CHECK (factor > 0),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_medicine_unit_conversion_medicine_id ON medicine_unit_conversion (medicine_id);

CREATE TABLE medicine_barcode (
    id           BIGSERIAL PRIMARY KEY,
    medicine_id  BIGINT       NOT NULL REFERENCES medicine (id),
    barcode      VARCHAR(32)  NOT NULL UNIQUE,
    remark       VARCHAR(128) NOT NULL DEFAULT '',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_medicine_barcode_medicine_id ON medicine_barcode (medicine_id);
