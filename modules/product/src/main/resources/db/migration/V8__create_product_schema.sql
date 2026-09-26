CREATE TABLE product_catalog_product (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    code varchar(80) NOT NULL UNIQUE,
    name varchar(200) NOT NULL,
    description varchar(2000),
    status varchar(20) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_product_catalog_product_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE'))
);

CREATE TABLE product_catalog_plan (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    product_id uuid NOT NULL,
    code varchar(80) NOT NULL,
    name varchar(200) NOT NULL,
    description varchar(2000),
    minimum_amount numeric(19, 2) NOT NULL,
    maximum_amount numeric(19, 2) NOT NULL,
    minimum_term_months integer NOT NULL,
    maximum_term_months integer NOT NULL,
    annual_interest_rate numeric(7, 4) NOT NULL,
    status varchar(20) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_catalog_plan_product FOREIGN KEY (product_id)
        REFERENCES product_catalog_product(id),
    CONSTRAINT uk_product_plan_code UNIQUE (product_id, code),
    CONSTRAINT ck_product_catalog_plan_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_product_catalog_plan_amount CHECK (
        minimum_amount >= 0 AND maximum_amount >= minimum_amount
    ),
    CONSTRAINT ck_product_catalog_plan_term CHECK (
        minimum_term_months > 0 AND maximum_term_months >= minimum_term_months
    ),
    CONSTRAINT ck_product_catalog_plan_interest CHECK (annual_interest_rate >= 0)
);

CREATE INDEX ix_product_catalog_plan_product_status
    ON product_catalog_plan(product_id, status, name);

CREATE TABLE product_plan_inquiry (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    plan_id uuid NOT NULL,
    code varchar(100) NOT NULL,
    title varchar(200) NOT NULL,
    stage_code varchar(100) NOT NULL,
    sequence_number integer NOT NULL,
    required boolean NOT NULL,
    enabled boolean NOT NULL,
    configuration_json text NOT NULL DEFAULT '{}',
    CONSTRAINT fk_product_plan_inquiry_plan FOREIGN KEY (plan_id)
        REFERENCES product_catalog_plan(id) ON DELETE CASCADE,
    CONSTRAINT uk_product_plan_inquiry_code UNIQUE (plan_id, code),
    CONSTRAINT uk_product_plan_inquiry_sequence UNIQUE (plan_id, sequence_number),
    CONSTRAINT ck_product_plan_inquiry_sequence CHECK (sequence_number > 0)
);

CREATE TABLE product_plan_guarantee (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    plan_id uuid NOT NULL,
    code varchar(100) NOT NULL,
    title varchar(200) NOT NULL,
    minimum_count integer NOT NULL,
    maximum_count integer NOT NULL,
    required boolean NOT NULL,
    enabled boolean NOT NULL,
    configuration_json text NOT NULL DEFAULT '{}',
    CONSTRAINT fk_product_plan_guarantee_plan FOREIGN KEY (plan_id)
        REFERENCES product_catalog_plan(id) ON DELETE CASCADE,
    CONSTRAINT uk_product_plan_guarantee_code UNIQUE (plan_id, code),
    CONSTRAINT ck_product_plan_guarantee_count CHECK (
        minimum_count >= 0 AND maximum_count >= minimum_count
    ),
    CONSTRAINT ck_product_plan_guarantee_required CHECK (NOT required OR minimum_count > 0)
);

CREATE TABLE product_plan_collateral (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    plan_id uuid NOT NULL,
    code varchar(100) NOT NULL,
    title varchar(200) NOT NULL,
    minimum_coverage_percent numeric(7, 2) NOT NULL,
    required boolean NOT NULL,
    enabled boolean NOT NULL,
    configuration_json text NOT NULL DEFAULT '{}',
    CONSTRAINT fk_product_plan_collateral_plan FOREIGN KEY (plan_id)
        REFERENCES product_catalog_plan(id) ON DELETE CASCADE,
    CONSTRAINT uk_product_plan_collateral_code UNIQUE (plan_id, code),
    CONSTRAINT ck_product_plan_collateral_coverage CHECK (minimum_coverage_percent >= 0)
);

CREATE TABLE product_plan_fee (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    plan_id uuid NOT NULL,
    code varchar(100) NOT NULL,
    title varchar(200) NOT NULL,
    amount numeric(19, 2) NOT NULL,
    currency varchar(3) NOT NULL,
    trigger_code varchar(100) NOT NULL,
    source_inquiry_code varchar(100),
    refundable boolean NOT NULL,
    enabled boolean NOT NULL,
    CONSTRAINT fk_product_plan_fee_plan FOREIGN KEY (plan_id)
        REFERENCES product_catalog_plan(id) ON DELETE CASCADE,
    CONSTRAINT uk_product_plan_fee_code UNIQUE (plan_id, code),
    CONSTRAINT ck_product_plan_fee_amount CHECK (amount >= 0)
);

CREATE TABLE product_plan_control (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    plan_id uuid NOT NULL,
    code varchar(100) NOT NULL,
    title varchar(200) NOT NULL,
    priority integer NOT NULL,
    control_type varchar(40) NOT NULL,
    minimum_value numeric(19, 4),
    maximum_value numeric(19, 4),
    source_inquiry_code varchar(100),
    failure_message varchar(500) NOT NULL,
    enabled boolean NOT NULL,
    CONSTRAINT fk_product_plan_control_plan FOREIGN KEY (plan_id)
        REFERENCES product_catalog_plan(id) ON DELETE CASCADE,
    CONSTRAINT uk_product_plan_control_code UNIQUE (plan_id, code),
    CONSTRAINT uk_product_plan_control_priority UNIQUE (plan_id, priority),
    CONSTRAINT ck_product_plan_control_priority CHECK (priority > 0),
    CONSTRAINT ck_product_plan_control_type CHECK (
        control_type IN ('AGE_RANGE', 'MINIMUM_CREDIT_RANK', 'NO_BAD_CHEQUE')
    ),
    CONSTRAINT ck_product_plan_control_values CHECK (
        (minimum_value IS NULL OR minimum_value >= 0)
        AND (maximum_value IS NULL OR maximum_value >= 0)
    ),
    CONSTRAINT ck_product_plan_control_configuration CHECK (
        (control_type = 'AGE_RANGE'
            AND source_inquiry_code IS NULL
            AND (minimum_value IS NOT NULL OR maximum_value IS NOT NULL)
            AND (minimum_value IS NULL OR maximum_value IS NULL OR maximum_value >= minimum_value))
        OR (control_type = 'MINIMUM_CREDIT_RANK'
            AND minimum_value IS NOT NULL
            AND maximum_value IS NULL
            AND source_inquiry_code IS NOT NULL)
        OR (control_type = 'NO_BAD_CHEQUE'
            AND minimum_value IS NULL
            AND maximum_value IS NULL
            AND source_inquiry_code IS NOT NULL)
    )
);

CREATE INDEX ix_product_plan_inquiry_order
    ON product_plan_inquiry(plan_id, enabled, sequence_number);

CREATE INDEX ix_product_plan_requirements
    ON product_plan_guarantee(plan_id, enabled);

CREATE INDEX ix_product_plan_collaterals
    ON product_plan_collateral(plan_id, enabled);

CREATE INDEX ix_product_plan_fees
    ON product_plan_fee(plan_id, enabled);

CREATE INDEX ix_product_plan_controls
    ON product_plan_control(plan_id, enabled, priority);
