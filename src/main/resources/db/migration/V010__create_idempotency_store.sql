-- V010: Create idempotency_store table

CREATE TABLE idempotency_store (
    idempotency_key  VARCHAR(64)  PRIMARY KEY,
    http_method      VARCHAR(10)  NOT NULL,
    request_path     VARCHAR(512) NOT NULL,
    response_status  INT          NOT NULL,
    response_body    TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_idempotency_expires ON idempotency_store (expires_at);
