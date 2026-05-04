-- V005: Create installments table

CREATE TABLE installment (
    id                UUID         PRIMARY KEY,
    loan_account_id   UUID         NOT NULL REFERENCES loan_account(id),
    installment_number INT         NOT NULL,
    due_date          DATE         NOT NULL,
    principal_due     NUMERIC(19,4) NOT NULL,
    interest_due      NUMERIC(19,4) NOT NULL,
    total_due         NUMERIC(19,4) NOT NULL,
    principal_paid    NUMERIC(19,4) NOT NULL DEFAULT 0,
    interest_paid     NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_paid        NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency          VARCHAR(3)   NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    UNIQUE (loan_account_id, installment_number)
);

CREATE INDEX idx_installment_loan_due ON installment (loan_account_id, due_date);
CREATE INDEX idx_installment_status ON installment (status);
