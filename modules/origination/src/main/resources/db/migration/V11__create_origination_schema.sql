CREATE TABLE origination_loan_application (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    customer_party_id uuid NOT NULL,
    national_code varchar(10) NOT NULL,
    birth_date date NOT NULL,
    plan_id uuid NOT NULL,
    plan_code varchar(80) NOT NULL,
    plan_name varchar(200) NOT NULL,
    requested_amount numeric(19, 2) NOT NULL,
    term_months integer NOT NULL,
    annual_interest_rate numeric(7, 4) NOT NULL,
    status varchar(50) NOT NULL,
    personal_profile_revision bigint,
    employment_profile_revision bigint,
    guarantee_information_json text,
    requires_guarantee boolean NOT NULL,
    requires_original_cheque boolean NOT NULL,
    requires_application_fee boolean NOT NULL,
    rejection_reason varchar(500),
    contract_signed_at timestamptz,
    completed_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT ck_origination_requested_amount CHECK (requested_amount > 0),
    CONSTRAINT ck_origination_term_months CHECK (term_months > 0),
    CONSTRAINT ck_origination_personal_profile_revision CHECK (
        personal_profile_revision IS NULL OR personal_profile_revision > 0
    ),
    CONSTRAINT ck_origination_employment_profile_revision CHECK (
        employment_profile_revision IS NULL OR employment_profile_revision > 0
    )
);

CREATE INDEX ix_origination_application_customer
    ON origination_loan_application(customer_party_id, created_at DESC);
CREATE INDEX ix_origination_application_status
    ON origination_loan_application(status, updated_at);

CREATE TABLE origination_application_control (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    application_id uuid NOT NULL,
    control_code varchar(100) NOT NULL,
    title varchar(200) NOT NULL,
    priority integer NOT NULL,
    control_type varchar(40) NOT NULL,
    minimum_value numeric(19, 4),
    maximum_value numeric(19, 4),
    source_inquiry_code varchar(100),
    failure_message varchar(500) NOT NULL,
    status varchar(40) NOT NULL,
    inquiry_request_id uuid,
    facts_json text,
    observed_value varchar(500),
    error_message varchar(1000),
    evaluated_at timestamptz,
    CONSTRAINT fk_origination_control_application FOREIGN KEY (application_id)
        REFERENCES origination_loan_application(id),
    CONSTRAINT uk_origination_control_code UNIQUE (application_id, control_code),
    CONSTRAINT uk_origination_control_priority UNIQUE (application_id, priority),
    CONSTRAINT ck_origination_control_priority CHECK (priority > 0)
);

CREATE UNIQUE INDEX uk_origination_control_inquiry_request
    ON origination_application_control(inquiry_request_id)
    WHERE inquiry_request_id IS NOT NULL;

CREATE INDEX ix_origination_control_status
    ON origination_application_control(application_id, status, priority);
