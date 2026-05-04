-- V004: Create loan_account table

CREATE TABLE loan_account (
    id                            UUID         PRIMARY KEY,
    application_id                UUID         NOT NULL UNIQUE REFERENCES loan_application(id),
    customer_id                   UUID         NOT NULL REFERENCES customer(id),
    product_id                    UUID         NOT NULL REFERENCES loan_product(id),
    principal_amount              NUMERIC(19,4) NOT NULL,
    principal_currency            VARCHAR(3)   NOT NULL,
    interest_rate_per_annum       NUMERIC(19,4) NOT NULL,
    interest_accrual_method       VARCHAR(20)  NOT NULL,
    tenure_months                 INT          NOT NULL,
    repayment_frequency           VARCHAR(20)  NOT NULL,
    grace_period_days             INT          NOT NULL,
    origination_fee_charge_method VARCHAR(30)  NOT NULL,
    allow_early_repayment         BOOLEAN      NOT NULL,
    overpayment_policy            VARCHAR(30)  NOT NULL,
    origination_fee_amount        NUMERIC(19,4) NOT NULL,
    origination_fee_currency      VARCHAR(3)   NOT NULL,
    total_disbursed_amount        NUMERIC(19,4) NOT NULL,
    total_disbursed_currency      VARCHAR(3)   NOT NULL,
    outstanding_balance_amount    NUMERIC(19,4) NOT NULL,
    outstanding_balance_currency  VARCHAR(3)   NOT NULL,
    credit_balance_amount         NUMERIC(19,4) NOT NULL DEFAULT 0,
    credit_balance_currency       VARCHAR(3)   NOT NULL,
    state                         VARCHAR(30)  NOT NULL,
    disbursed_at                  TIMESTAMPTZ,
    closed_at                     TIMESTAMPTZ,
    created_at                    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by                    VARCHAR(100) NOT NULL DEFAULT 'system',
    version                       BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_loan_account_customer ON loan_account (customer_id);
CREATE INDEX idx_loan_account_state ON loan_account (state);
