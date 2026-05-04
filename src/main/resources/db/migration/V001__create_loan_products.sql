-- V001: Create loan_product and fee_definition tables

CREATE TABLE loan_product (
    id                            UUID         PRIMARY KEY,
    name                          VARCHAR(100) NOT NULL UNIQUE,
    description                   VARCHAR(500),
    interest_rate_per_annum       NUMERIC(19,4) NOT NULL,
    interest_accrual_method       VARCHAR(20)  NOT NULL DEFAULT 'PRE_COMPUTED',
    min_tenure_months             INT          NOT NULL,
    max_tenure_months             INT          NOT NULL,
    min_principal_amount          NUMERIC(19,4) NOT NULL,
    min_principal_currency        VARCHAR(3)   NOT NULL,
    max_principal_amount          NUMERIC(19,4) NOT NULL,
    max_principal_currency        VARCHAR(3)   NOT NULL,
    repayment_frequency           VARCHAR(20)  NOT NULL,
    grace_period_days             INT          NOT NULL DEFAULT 0,
    origination_fee_charge_method VARCHAR(30)  NOT NULL DEFAULT 'DEDUCTED_FROM_DISBURSEMENT',
    allow_early_repayment         BOOLEAN      NOT NULL DEFAULT true,
    overpayment_policy            VARCHAR(30)  NOT NULL DEFAULT 'REJECT',
    active                        BOOLEAN      NOT NULL DEFAULT true,
    deleted_at                    TIMESTAMPTZ,
    created_at                    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version                       BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE fee_definition (
    id                 UUID         PRIMARY KEY,
    product_id         UUID         NOT NULL REFERENCES loan_product(id),
    fee_type           VARCHAR(30)  NOT NULL,
    calculation_method VARCHAR(30)  NOT NULL,
    amount             NUMERIC(19,4) NOT NULL,
    currency           VARCHAR(3)   NOT NULL,
    UNIQUE (product_id, fee_type)
);
