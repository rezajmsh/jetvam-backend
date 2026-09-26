CREATE TABLE payment_fee_obligation (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    customer_party_id UUID NOT NULL,
    reference_type VARCHAR(50) NOT NULL,
    reference_id UUID NOT NULL,
    fee_code VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(30) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    activation_key VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_payment_fee_reference_code UNIQUE (reference_type, reference_id, fee_code),
    CONSTRAINT ck_payment_fee_amount CHECK (amount >= 0)
);

CREATE INDEX ix_payment_fee_reference
    ON payment_fee_obligation(reference_type, reference_id, category, status);

CREATE INDEX ix_payment_fee_activation
    ON payment_fee_obligation(reference_type, reference_id, activation_key, status);

CREATE TABLE payment_attempt (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    fee_obligation_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider_reference VARCHAR(150),
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_payment_attempt_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_payment_attempt_fee FOREIGN KEY (fee_obligation_id)
        REFERENCES payment_fee_obligation(id)
);
