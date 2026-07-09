CREATE TABLE visit_embedding (
    id                   BIGSERIAL PRIMARY KEY,
    visit_id             BIGINT NOT NULL UNIQUE REFERENCES clinic_visit (id),
    embedding            vector(1024) NOT NULL,
    text_summary         TEXT NOT NULL,
    embedding_model      VARCHAR(128) NOT NULL,
    embedding_dimensions INT NOT NULL DEFAULT 1024,
    source_updated_at    TIMESTAMPTZ NOT NULL,
    synced_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_visit_embedding_synced_at ON visit_embedding (synced_at);
