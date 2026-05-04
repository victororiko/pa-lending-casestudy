-- V902: Seed demo loans
-- Creates one active loan for Wanjiku Kamau with the Mkopo wa Muda Mfupi product

-- Loan Application (approved)
INSERT INTO loan_application (id, customer_id, product_id, requested_principal_amount,
    requested_principal_currency, requested_tenure_months, status, approved_at,
    snapshot_interest_rate, snapshot_fee_schedule, created_at, updated_at, version)
VALUES (
    'd1e2f3a4-b5c6-7890-abcd-ef1234567890',
    'c1d2e3f4-a5b6-7890-cdef-123456789001',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    100000.0000, 'KES', 6, 'APPROVED', NOW(),
    14.00,
    '[{"feeType":"ORIGINATION_FEE","calculationMethod":"PERCENTAGE_OF_PRINCIPAL","amount":"2.5000","currency":"KES"},{"feeType":"LATE_PAYMENT_FEE","calculationMethod":"FLAT","amount":"2500.0000","currency":"KES"}]',
    NOW(), NOW(), 1
);

-- Loan Account (active, disbursed)
INSERT INTO loan_account (id, application_id, customer_id, product_id,
    principal_amount, principal_currency, interest_rate_per_annum,
    interest_accrual_method, tenure_months, repayment_frequency,
    grace_period_days, origination_fee_charge_method, allow_early_repayment,
    overpayment_policy, origination_fee_amount, origination_fee_currency,
    total_disbursed_amount, total_disbursed_currency,
    outstanding_balance_amount, outstanding_balance_currency,
    credit_balance_amount, credit_balance_currency,
    state, disbursed_at, created_at, updated_at, created_by, version)
VALUES (
    'e2f3a4b5-c6d7-8901-bcde-f12345678901',
    'd1e2f3a4-b5c6-7890-abcd-ef1234567890',
    'c1d2e3f4-a5b6-7890-cdef-123456789001',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    100000.0000, 'KES', 14.00,
    'PRE_COMPUTED', 6, 'MONTHLY',
    3, 'DEDUCTED_FROM_DISBURSEMENT', true,
    'REJECT', 2500.0000, 'KES',
    97500.0000, 'KES',
    104175.3600, 'KES',
    0.0000, 'KES',
    'ACTIVE', NOW(), NOW(), NOW(), 'system', 0
);

-- Installments for the demo loan (6 months at 14% APR on KES 100,000)
INSERT INTO installment (id, loan_account_id, installment_number, due_date,
    principal_due, interest_due, total_due, principal_paid, interest_paid, total_paid,
    currency, status)
VALUES
    ('f1111111-1111-1111-1111-111111111111', 'e2f3a4b5-c6d7-8901-bcde-f12345678901',
     1, CURRENT_DATE + INTERVAL '1 month', 16195.7300, 1166.6700, 17362.4000, 0, 0, 0, 'KES', 'PENDING'),
    ('f2222222-2222-2222-2222-222222222222', 'e2f3a4b5-c6d7-8901-bcde-f12345678901',
     2, CURRENT_DATE + INTERVAL '2 months', 16384.6200, 977.7800, 17362.4000, 0, 0, 0, 'KES', 'PENDING'),
    ('f3333333-3333-3333-3333-333333333333', 'e2f3a4b5-c6d7-8901-bcde-f12345678901',
     3, CURRENT_DATE + INTERVAL '3 months', 16575.6800, 786.7200, 17362.4000, 0, 0, 0, 'KES', 'PENDING'),
    ('f4444444-4444-4444-4444-444444444444', 'e2f3a4b5-c6d7-8901-bcde-f12345678901',
     4, CURRENT_DATE + INTERVAL '4 months', 16768.9400, 593.4600, 17362.4000, 0, 0, 0, 'KES', 'PENDING'),
    ('f5555555-5555-5555-5555-555555555555', 'e2f3a4b5-c6d7-8901-bcde-f12345678901',
     5, CURRENT_DATE + INTERVAL '5 months', 16964.4200, 397.9800, 17362.4000, 0, 0, 0, 'KES', 'PENDING'),
    ('f6666666-6666-6666-6666-666666666666', 'e2f3a4b5-c6d7-8901-bcde-f12345678901',
     6, CURRENT_DATE + INTERVAL '6 months', 17110.6100, 252.9500, 17363.5600, 0, 0, 0, 'KES', 'PENDING');
