-- V901: Seed customers

INSERT INTO customer (id, first_name, last_name, email, phone_number, national_id,
    credit_limit_amount, credit_limit_currency, max_active_loans, created_at, updated_at, version)
VALUES
    ('c1d2e3f4-a5b6-7890-cdef-123456789001', 'Wanjiku', 'Kamau', 'wanjiku.kamau@example.co.ke',
     '+254712345678', '12345678', 10000000.0000, 'KES', 3, NOW(), NOW(), 0),
    ('c1d2e3f4-a5b6-7890-cdef-123456789002', 'Otieno', 'Odhiambo', 'otieno.odhiambo@example.co.ke',
     '+254723456789', '87654321', 5000000.0000, 'KES', 2, NOW(), NOW(), 0),
    ('c1d2e3f4-a5b6-7890-cdef-123456789003', 'Aisha', 'Mwangi', 'aisha.mwangi@example.co.ke',
     '+254734567890', '11223344', 2500000.0000, 'KES', 1, NOW(), NOW(), 0);
