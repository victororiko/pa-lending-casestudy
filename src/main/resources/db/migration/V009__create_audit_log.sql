-- V009: Create audit_log table

CREATE TABLE audit_log (
    id              UUID         PRIMARY KEY,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       UUID         NOT NULL,
    action          VARCHAR(20)  NOT NULL,
    before_snapshot JSONB,
    after_snapshot  JSONB        NOT NULL,
    changed_fields  TEXT[],
    performed_by    VARCHAR(100) NOT NULL,
    performed_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_performed_at ON audit_log (performed_at);
