-- V002: Create customers table

CREATE TABLE customer (
    id                    UUID         PRIMARY KEY,
    first_name            VARCHAR(100) NOT NULL,
    last_name             VARCHAR(100) NOT NULL,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    phone_number          VARCHAR(20)  NOT NULL UNIQUE,
    national_id           VARCHAR(50)  NOT NULL UNIQUE,
    credit_limit_amount   NUMERIC(19,4) NOT NULL,
    credit_limit_currency VARCHAR(3)   NOT NULL,
    max_active_loans      INT          NOT NULL DEFAULT 3,
    deleted_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version               BIGINT       NOT NULL DEFAULT 0
);
