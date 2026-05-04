-- V006: Create repayments table

CREATE TABLE repayment (
    id                UUID         PRIMARY KEY,
    loan_account_id   UUID         NOT NULL REFERENCES loan_account(id),
    amount            NUMERIC(19,4) NOT NULL,
    currency          VARCHAR(3)   NOT NULL,
    received_at       TIMESTAMPTZ  NOT NULL,
    idempotency_key   VARCHAR(64)  NOT NULL UNIQUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_repayment_loan ON repayment (loan_account_id);
