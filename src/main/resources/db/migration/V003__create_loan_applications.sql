-- V003: Create loan_application table

CREATE TABLE loan_application (
    id                            UUID         PRIMARY KEY,
    customer_id                   UUID         NOT NULL REFERENCES customer(id),
    product_id                    UUID         NOT NULL REFERENCES loan_product(id),
    requested_principal_amount    NUMERIC(19,4) NOT NULL,
    requested_principal_currency  VARCHAR(3)   NOT NULL,
    requested_tenure_months       INT          NOT NULL,
    status                        VARCHAR(20)  NOT NULL,
    rejection_reason              TEXT,
    approved_at                   TIMESTAMPTZ,
    snapshot_interest_rate        NUMERIC(19,4),
    snapshot_fee_schedule         JSONB,
    created_at                    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version                       BIGINT       NOT NULL DEFAULT 0
);
