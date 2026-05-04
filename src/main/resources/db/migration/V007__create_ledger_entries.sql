-- V007: Create ledger_entries table

CREATE TABLE ledger_entry (
    id                UUID         PRIMARY KEY,
    loan_account_id   UUID         NOT NULL REFERENCES loan_account(id),
    entry_type        VARCHAR(30)  NOT NULL,
    amount            NUMERIC(19,4) NOT NULL,
    currency          VARCHAR(3)   NOT NULL,
    direction         VARCHAR(10)  NOT NULL,
    description       TEXT,
    reference_id      UUID,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_loan ON ledger_entry (loan_account_id, created_at);
