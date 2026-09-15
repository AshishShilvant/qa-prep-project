INSERT INTO accounts (account_id, account_holder, balance, status) VALUES
    ('ACC1001', 'John Doe',   5000.00, 'ACTIVE'),
    ('ACC1002', 'Jane Smith', 12500.50, 'ACTIVE'),
    ('ACC1003', 'Old Holder', 0.00, 'CLOSED');

INSERT INTO transactions (account_id, amount, transaction_type) VALUES
    ('ACC1001', 250.00, 'CREDIT'),
    ('ACC1001', 100.00, 'DEBIT'),
    ('ACC1002', 500.00, 'CREDIT');
