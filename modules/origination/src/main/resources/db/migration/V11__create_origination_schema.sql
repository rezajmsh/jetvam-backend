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
    requires_application_fee boolean NOT NULL,
    rejection_reason varchar(500),
    contract_signed_at timestamptz,
    completed_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT ck_origination_requested_amount CHECK (requested_amount > 0),
    CONSTRAINT ck_origination_term_months CHECK (term_months > 0)
);

CREATE INDEX ix_origination_application_customer
    ON origination_loan_application(customer_party_id, created_at DESC);
CREATE INDEX ix_origination_application_status
    ON origination_loan_application(status, updated_at);

CREATE TABLE origination_application_profile_reference_option (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    category varchar(100) NOT NULL,
    code varchar(100) NOT NULL,
    label varchar(200) NOT NULL,
    display_order integer NOT NULL,
    active boolean NOT NULL DEFAULT true,
    CONSTRAINT uk_origination_reference_code UNIQUE (category, code),
    CONSTRAINT ck_origination_reference_order CHECK (display_order > 0)
);

CREATE INDEX ix_origination_profile_reference_active
    ON origination_application_profile_reference_option(category, active, display_order);

INSERT INTO origination_application_profile_reference_option (id, category, code, label, display_order) VALUES
    ('10000000-0000-0000-0000-000000000001', 'EDUCATION_LEVEL', 'DIPLOMA', 'دیپلم', 10),
    ('10000000-0000-0000-0000-000000000002', 'EDUCATION_LEVEL', 'ASSOCIATE', 'کاردانی', 20),
    ('10000000-0000-0000-0000-000000000003', 'EDUCATION_LEVEL', 'BACHELOR', 'کارشناسی', 30),
    ('10000000-0000-0000-0000-000000000004', 'EDUCATION_LEVEL', 'MASTER', 'کارشناسی ارشد', 40),
    ('10000000-0000-0000-0000-000000000005', 'EDUCATION_LEVEL', 'DOCTORATE', 'دکتری', 50),
    ('10000000-0000-0000-0000-000000000011', 'EMPLOYMENT_STATUS', 'EMPLOYEE', 'کارمند', 10),
    ('10000000-0000-0000-0000-000000000012', 'EMPLOYMENT_STATUS', 'SELF_EMPLOYED', 'خویش‌فرما', 20),
    ('10000000-0000-0000-0000-000000000013', 'EMPLOYMENT_STATUS', 'RETIRED', 'بازنشسته', 30),
    ('10000000-0000-0000-0000-000000000014', 'EMPLOYMENT_STATUS', 'UNEMPLOYED', 'فاقد شغل', 40);

CREATE TABLE origination_application_employment (
    application_id uuid PRIMARY KEY,
    education_category varchar(100) NOT NULL DEFAULT 'EDUCATION_LEVEL',
    education_code varchar(100) NOT NULL,
    employment_category varchar(100) NOT NULL DEFAULT 'EMPLOYMENT_STATUS',
    employment_code varchar(100) NOT NULL,
    monthly_income numeric(19, 2) NOT NULL,
    CONSTRAINT fk_origination_employment_application FOREIGN KEY (application_id)
        REFERENCES origination_loan_application(id),
    CONSTRAINT fk_origination_employment_education FOREIGN KEY (education_category, education_code)
        REFERENCES origination_application_profile_reference_option(category, code),
    CONSTRAINT fk_origination_employment_status FOREIGN KEY (employment_category, employment_code)
        REFERENCES origination_application_profile_reference_option(category, code),
    CONSTRAINT ck_origination_monthly_income CHECK (monthly_income >= 0)
);

CREATE TABLE origination_application_employment_document_requirement (
    id uuid PRIMARY KEY,
    employment_category varchar(100) NOT NULL DEFAULT 'EMPLOYMENT_STATUS',
    employment_code varchar(100) NOT NULL,
    document_type_code varchar(100) NOT NULL,
    title varchar(200) NOT NULL,
    required boolean NOT NULL,
    display_order integer NOT NULL,
    CONSTRAINT uk_origination_employment_document UNIQUE (employment_code, document_type_code),
    CONSTRAINT fk_origination_document_employment FOREIGN KEY (employment_category, employment_code)
        REFERENCES origination_application_profile_reference_option(category, code)
);

INSERT INTO origination_application_employment_document_requirement
    (id, employment_code, document_type_code, title, required, display_order) VALUES
    ('11000000-0000-0000-0000-000000000001', 'EMPLOYEE', 'SALARY_SLIP', 'فیش حقوقی', true, 10),
    ('11000000-0000-0000-0000-000000000002', 'EMPLOYEE', 'EMPLOYMENT_CERTIFICATE', 'گواهی اشتغال به کار', true, 20),
    ('11000000-0000-0000-0000-000000000003', 'SELF_EMPLOYED', 'BUSINESS_LICENSE', 'جواز کسب یا مجوز فعالیت', true, 10),
    ('11000000-0000-0000-0000-000000000004', 'SELF_EMPLOYED', 'BANK_STATEMENT', 'گردش حساب', true, 20),
    ('11000000-0000-0000-0000-000000000005', 'RETIRED', 'RETIREMENT_ORDER', 'حکم بازنشستگی', true, 10),
    ('11000000-0000-0000-0000-000000000006', 'RETIRED', 'SALARY_SLIP', 'فیش حقوقی', true, 20);

CREATE TABLE origination_application_guarantor (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    application_id uuid NOT NULL,
    sequence_number integer NOT NULL,
    required boolean NOT NULL,
    party_id uuid,
    national_code varchar(10),
    mobile varchar(11),
    status varchar(40) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_origination_guarantor_application FOREIGN KEY (application_id)
        REFERENCES origination_loan_application(id),
    CONSTRAINT uk_origination_guarantor_sequence UNIQUE (application_id, sequence_number),
    CONSTRAINT ck_origination_guarantor_sequence CHECK (sequence_number > 0)
);

CREATE INDEX ix_origination_guarantor_party
    ON origination_application_guarantor(party_id, status);

CREATE TABLE origination_application_collateral (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    application_id uuid NOT NULL,
    provider_type varchar(20) NOT NULL,
    provider_party_id uuid,
    guarantor_id uuid,
    collateral_type_id uuid NOT NULL,
    collateral_type_code varchar(100) NOT NULL,
    collateral_type_title varchar(200) NOT NULL,
    handler_code varchar(100) NOT NULL,
    minimum_coverage_percent numeric(7, 2) NOT NULL,
    required boolean NOT NULL,
    requires_physical_delivery boolean NOT NULL,
    status varchar(40) NOT NULL,
    information_completed_at timestamptz,
    original_received_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_origination_collateral_application FOREIGN KEY (application_id)
        REFERENCES origination_loan_application(id),
    CONSTRAINT fk_origination_collateral_guarantor FOREIGN KEY (guarantor_id)
        REFERENCES origination_application_guarantor(id),
    CONSTRAINT ck_origination_collateral_coverage CHECK (minimum_coverage_percent >= 0),
    CONSTRAINT ck_origination_collateral_provider CHECK (
        (provider_type = 'APPLICANT' AND guarantor_id IS NULL AND provider_party_id IS NOT NULL)
        OR (provider_type = 'GUARANTOR' AND guarantor_id IS NOT NULL)
    )
);

CREATE INDEX ix_origination_collateral_application_status
    ON origination_application_collateral(application_id, provider_type, status);
CREATE INDEX ix_origination_collateral_provider
    ON origination_application_collateral(provider_party_id, status);

CREATE TABLE origination_application_collateral_document_requirement (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    application_collateral_id uuid NOT NULL,
    document_type_id uuid NOT NULL,
    document_type_code varchar(100) NOT NULL,
    document_type_title varchar(200) NOT NULL,
    allowed_content_types varchar(1000) NOT NULL,
    maximum_size_bytes bigint NOT NULL,
    required boolean NOT NULL,
    minimum_count integer NOT NULL,
    maximum_count integer NOT NULL,
    display_order integer NOT NULL,
    CONSTRAINT fk_origination_collateral_document_requirement FOREIGN KEY (application_collateral_id)
        REFERENCES origination_application_collateral(id),
    CONSTRAINT uk_origination_collateral_document_requirement
        UNIQUE (application_collateral_id, document_type_id),
    CONSTRAINT ck_origination_collateral_document_counts
        CHECK (minimum_count >= 0 AND maximum_count > 0 AND maximum_count >= minimum_count),
    CONSTRAINT ck_origination_collateral_document_size CHECK (maximum_size_bytes > 0),
    CONSTRAINT ck_origination_collateral_document_order CHECK (display_order > 0)
);

CREATE TABLE origination_application_document (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    application_id uuid NOT NULL,
    collateral_document_requirement_id uuid NOT NULL,
    storage_key varchar(500) NOT NULL,
    original_filename varchar(255) NOT NULL,
    content_type varchar(150) NOT NULL,
    size_bytes bigint NOT NULL,
    checksum_sha256 varchar(64) NOT NULL,
    uploaded_by_party_id uuid NOT NULL,
    status varchar(30) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_origination_document_application FOREIGN KEY (application_id)
        REFERENCES origination_loan_application(id),
    CONSTRAINT fk_origination_document_collateral_requirement FOREIGN KEY (collateral_document_requirement_id)
        REFERENCES origination_application_collateral_document_requirement(id),
    CONSTRAINT uk_origination_document_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_origination_document_size CHECK (size_bytes > 0)
);

CREATE INDEX ix_origination_document_application
    ON origination_application_document(application_id, collateral_document_requirement_id, status);

CREATE TABLE origination_application_cheque_collateral (
    application_collateral_id uuid PRIMARY KEY,
    sayad_id varchar(32) NOT NULL,
    bank_code varchar(20) NOT NULL,
    cheque_number varchar(50) NOT NULL,
    cheque_serial varchar(50) NOT NULL,
    cheque_date date NOT NULL,
    amount numeric(19, 2) NOT NULL,
    CONSTRAINT fk_origination_cheque_collateral FOREIGN KEY (application_collateral_id)
        REFERENCES origination_application_collateral(id),
    CONSTRAINT uk_origination_cheque_sayad UNIQUE (sayad_id),
    CONSTRAINT ck_origination_cheque_amount CHECK (amount > 0)
);

CREATE TABLE origination_application_control (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    application_id uuid NOT NULL,
    subject_type varchar(20) NOT NULL,
    subject_party_id uuid,
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
    CONSTRAINT uk_origination_control_code UNIQUE (application_id, subject_type, control_code),
    CONSTRAINT uk_origination_control_priority UNIQUE (application_id, subject_type, priority),
    CONSTRAINT ck_origination_control_priority CHECK (priority > 0)
);

CREATE UNIQUE INDEX uk_origination_control_inquiry_request
    ON origination_application_control(inquiry_request_id)
    WHERE inquiry_request_id IS NOT NULL;

CREATE INDEX ix_origination_control_status
    ON origination_application_control(application_id, subject_type, status, priority);
