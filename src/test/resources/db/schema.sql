DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;

CREATE TABLE accounts (
    account_id     VARCHAR(20)    PRIMARY KEY,
    account_holder VARCHAR(100)   NOT NULL,
    balance        DECIMAL(12,2)  NOT NULL,
    status         VARCHAR(20)    NOT NULL
);

CREATE TABLE transactions (
    transaction_id   INT AUTO_INCREMENT PRIMARY KEY,
    account_id       VARCHAR(20)    NOT NULL,
    amount           DECIMAL(12,2)  NOT NULL,
    transaction_type VARCHAR(10)    NOT NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);
