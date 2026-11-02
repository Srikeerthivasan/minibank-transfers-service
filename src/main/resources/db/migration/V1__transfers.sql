CREATE TABLE transfers (
 id CHAR(36) PRIMARY KEY, idempotency_key VARCHAR(100) NOT NULL UNIQUE,
 from_account VARCHAR(30) NOT NULL, to_account VARCHAR(30) NOT NULL,
 amount NUMERIC(14,2) NOT NULL CHECK(amount>0), currency CHAR(3) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','COMPLETED','REJECTED')),
 reason VARCHAR(50), attempts INTEGER NOT NULL DEFAULT 0,
 lease_until TIMESTAMP(6), next_attempt_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CHECK(from_account<>to_account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE INDEX pending_transfers ON transfers(status,next_attempt_at,created_at);
CREATE INDEX transfers_from ON transfers(from_account,created_at);
CREATE INDEX transfers_to ON transfers(to_account,created_at);
