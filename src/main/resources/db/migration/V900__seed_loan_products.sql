-- V900: Seed loan products

-- Product 1: Short-Term Consumer Loan (Mkopo wa Muda Mfupi)
INSERT INTO loan_product (id, name, description, interest_rate_per_annum, interest_accrual_method,
    min_tenure_months, max_tenure_months, min_principal_amount, min_principal_currency,
    max_principal_amount, max_principal_currency, repayment_frequency,
    grace_period_days, origination_fee_charge_method, allow_early_repayment,
    overpayment_policy, active, created_at, updated_at, version)
VALUES (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'Mkopo wa Muda Mfupi',
    '6-month consumer loan with flat origination fee for personal use',
    14.00, 'PRE_COMPUTED',
    3, 12, 10000.0000, 'KES', 500000.0000, 'KES', 'MONTHLY',
    3, 'DEDUCTED_FROM_DISBURSEMENT', true,
    'REJECT', true, NOW(), NOW(), 0
);

INSERT INTO fee_definition (id, product_id, fee_type, calculation_method, amount, currency)
VALUES
    ('f1a2b3c4-d5e6-7890-abcd-ef1234567891', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
     'ORIGINATION_FEE', 'PERCENTAGE_OF_PRINCIPAL', 2.5000, 'KES'),
    ('f1a2b3c4-d5e6-7890-abcd-ef1234567892', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
     'LATE_PAYMENT_FEE', 'FLAT', 2500.0000, 'KES');

-- Product 2: Long-Term Installment Loan (Mkopo wa Muda Mrefu)
INSERT INTO loan_product (id, name, description, interest_rate_per_annum, interest_accrual_method,
    min_tenure_months, max_tenure_months, min_principal_amount, min_principal_currency,
    max_principal_amount, max_principal_currency, repayment_frequency,
    grace_period_days, origination_fee_charge_method, allow_early_repayment,
    overpayment_policy, active, created_at, updated_at, version)
VALUES (
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    'Mkopo wa Muda Mrefu',
    '12-60 month installment loan with early repayment allowed and prepayment penalty',
    12.00, 'PRE_COMPUTED',
    12, 60, 50000.0000, 'KES', 5000000.0000, 'KES', 'MONTHLY',
    5, 'ADDED_TO_BALANCE', true,
    'APPLY_TO_NEXT_INSTALLMENT', true, NOW(), NOW(), 0
);

INSERT INTO fee_definition (id, product_id, fee_type, calculation_method, amount, currency)
VALUES
    ('f2b3c4d5-e6f7-8901-bcde-f12345678902', 'b2c3d4e5-f6a7-8901-bcde-f12345678901',
     'ORIGINATION_FEE', 'FLAT', 5000.0000, 'KES'),
    ('f2b3c4d5-e6f7-8901-bcde-f12345678903', 'b2c3d4e5-f6a7-8901-bcde-f12345678901',
     'LATE_PAYMENT_FEE', 'FLAT', 5000.0000, 'KES'),
    ('f2b3c4d5-e6f7-8901-bcde-f12345678904', 'b2c3d4e5-f6a7-8901-bcde-f12345678901',
     'PREPAYMENT_PENALTY', 'PERCENTAGE_OF_OUTSTANDING', 2.0000, 'KES');
