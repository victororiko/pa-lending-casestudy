-- V008: Create notification_records table

CREATE TABLE notification_record (
    id                UUID         PRIMARY KEY,
    customer_id       UUID         NOT NULL REFERENCES customer(id),
    loan_account_id   UUID         REFERENCES loan_account(id),
    channel           VARCHAR(20)  NOT NULL,
    event_type        VARCHAR(50)  NOT NULL,
    template_key      VARCHAR(100) NOT NULL,
    payload           TEXT,
    status            VARCHAR(20)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_customer ON notification_record (customer_id);
