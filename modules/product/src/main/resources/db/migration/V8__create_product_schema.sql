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

CREATE TABLE product_document_type (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    code varchar(100) NOT NULL UNIQUE,
    title varchar(200) NOT NULL,
    allowed_content_types varchar(1000) NOT NULL,
    maximum_size_bytes bigint NOT NULL,
    active boolean NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_product_document_maximum_size CHECK (maximum_size_bytes > 0)
);

CREATE TABLE product_collateral_type (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    code varchar(100) NOT NULL UNIQUE,
    title varchar(200) NOT NULL,
    handler_code varchar(100) NOT NULL,
    requires_physical_delivery boolean NOT NULL,
    description varchar(1000),
    active boolean NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_collateral_document_requirement (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    collateral_type_id uuid NOT NULL,
    document_type_id uuid NOT NULL,
    required boolean NOT NULL,
    minimum_count integer NOT NULL,
    maximum_count integer NOT NULL,
    display_order integer NOT NULL,
    CONSTRAINT fk_product_collateral_document_collateral FOREIGN KEY (collateral_type_id)
        REFERENCES product_collateral_type(id),
    CONSTRAINT fk_product_collateral_document_type FOREIGN KEY (document_type_id)
        REFERENCES product_document_type(id),
    CONSTRAINT uk_product_collateral_document UNIQUE (collateral_type_id, document_type_id),
    CONSTRAINT ck_product_collateral_document_count CHECK (
        minimum_count >= 0 AND maximum_count >= minimum_count AND maximum_count > 0
    ),
    CONSTRAINT ck_product_collateral_document_order CHECK (display_order > 0)
);

CREATE TABLE product_fee_definition (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    code varchar(100) NOT NULL UNIQUE,
    title varchar(200) NOT NULL,
    category varchar(30) NOT NULL,
    amount numeric(19, 2) NOT NULL,
    currency varchar(3) NOT NULL,
    trigger_code varchar(100) NOT NULL,
    source_inquiry_code varchar(100),
    refundable boolean NOT NULL,
    active boolean NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_product_fee_definition_amount CHECK (amount >= 0),
    CONSTRAINT ck_product_fee_definition_category CHECK (category IN ('INQUIRY', 'APPLICATION'))
);

INSERT INTO product_collateral_type
    (id, code, title, handler_code, requires_physical_delivery, description, active) VALUES
    ('08000000-0000-0000-0000-000000000001', 'SAYAD_CHEQUE', 'چک صیادی', 'SAYAD_CHEQUE', true, 'چک ثبت‌شده در سامانه صیاد به همراه تصویر و کنترل اصالت', true),
    ('08000000-0000-0000-0000-000000000002', 'PROMISSORY_NOTE', 'سفته', 'PROMISSORY_NOTE', true, 'سفته الکترونیکی یا فیزیکی', true),
    ('08000000-0000-0000-0000-000000000003', 'PROPERTY_DOCUMENT', 'سند ملکی', 'PROPERTY_DOCUMENT', true, 'وثیقه ملکی قابل ارزیابی', false);

INSERT INTO product_document_type
    (id, code, title, allowed_content_types, maximum_size_bytes, active) VALUES
    ('08200000-0000-0000-0000-000000000001', 'CHEQUE_FRONT_IMAGE', 'تصویر روی چک', 'image/jpeg,image/png,application/pdf', 10485760, true),
    ('08200000-0000-0000-0000-000000000002', 'CHEQUE_BACK_IMAGE', 'تصویر پشت چک', 'image/jpeg,image/png,application/pdf', 10485760, true),
    ('08200000-0000-0000-0000-000000000003', 'PROMISSORY_NOTE_IMAGE', 'تصویر سفته', 'image/jpeg,image/png,application/pdf', 10485760, true),
    ('08200000-0000-0000-0000-000000000004', 'PROPERTY_DEED_IMAGE', 'تصویر سند ملکی', 'image/jpeg,image/png,application/pdf', 20971520, true),
    ('08200000-0000-0000-0000-000000000005', 'VALUATION_REPORT', 'گزارش ارزیابی وثیقه', 'application/pdf,image/jpeg,image/png', 20971520, true);

INSERT INTO product_collateral_document_requirement
    (id, collateral_type_id, document_type_id, required, minimum_count, maximum_count, display_order) VALUES
    ('08300000-0000-0000-0000-000000000001', '08000000-0000-0000-0000-000000000001', '08200000-0000-0000-0000-000000000001', true, 1, 1, 10),
    ('08300000-0000-0000-0000-000000000002', '08000000-0000-0000-0000-000000000001', '08200000-0000-0000-0000-000000000002', false, 0, 1, 20),
    ('08300000-0000-0000-0000-000000000003', '08000000-0000-0000-0000-000000000002', '08200000-0000-0000-0000-000000000003', true, 1, 2, 10),
    ('08300000-0000-0000-0000-000000000004', '08000000-0000-0000-0000-000000000003', '08200000-0000-0000-0000-000000000004', true, 1, 10, 10),
    ('08300000-0000-0000-0000-000000000005', '08000000-0000-0000-0000-000000000003', '08200000-0000-0000-0000-000000000005', true, 1, 1, 20);

INSERT INTO product_fee_definition
    (id, code, title, category, amount, currency, trigger_code, source_inquiry_code, refundable, active) VALUES
    ('08100000-0000-0000-0000-000000000001', 'BANK_ACCOUNT_INQUIRY_FEE', 'کارمزد استعلام وضعیت حساب بانکی', 'INQUIRY', 150000, 'IRR', 'BANK_ACCOUNT_STATUS_INQUIRY', 'BANK_ACCOUNT_STATUS_INQUIRY', false, true),
    ('08100000-0000-0000-0000-000000000002', 'BANKING_FACILITIES_INQUIRY_FEE', 'کارمزد استعلام تسهیلات و تعهدات بانکی', 'INQUIRY', 200000, 'IRR', 'BANKING_FACILITIES_INQUIRY', 'BANKING_FACILITIES_INQUIRY', false, true),
    ('08100000-0000-0000-0000-000000000003', 'BAD_CHEQUE_INQUIRY_FEE', 'کارمزد استعلام چک برگشتی', 'INQUIRY', 150000, 'IRR', 'BAD_CHEQUE_INQUIRY', 'BAD_CHEQUE_INQUIRY', false, true),
    ('08100000-0000-0000-0000-000000000004', 'CREDIT_RATING_INQUIRY_FEE', 'کارمزد رتبه اعتباری', 'INQUIRY', 300000, 'IRR', 'CREDIT_RATING_INQUIRY', 'CREDIT_RATING_INQUIRY', false, true),
    ('08100000-0000-0000-0000-000000000005', 'JET_CLUB_MEMBERSHIP_FEE', 'حق عضویت جت‌کلاب', 'APPLICATION', 0, 'IRR', 'APPLICATION_APPROVED', NULL, false, true);

CREATE TABLE product_control_definition (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    code varchar(100) NOT NULL UNIQUE,
    title varchar(200) NOT NULL,
    evaluator_type varchar(50) NOT NULL,
    inquiry_code varchar(100),
    default_failure_message varchar(500) NOT NULL,
    active boolean NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_control_parameter_definition (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    control_definition_id uuid NOT NULL,
    code varchar(100) NOT NULL,
    title varchar(200) NOT NULL,
    value_role varchar(20) NOT NULL,
    data_type varchar(20) NOT NULL,
    required boolean NOT NULL,
    display_order integer NOT NULL,
    CONSTRAINT fk_product_control_parameter_definition FOREIGN KEY (control_definition_id)
        REFERENCES product_control_definition(id) ON DELETE CASCADE,
    CONSTRAINT uk_product_control_parameter_code UNIQUE (control_definition_id, code),
    CONSTRAINT uk_product_control_parameter_role UNIQUE (control_definition_id, value_role),
    CONSTRAINT ck_product_control_parameter_role CHECK (value_role IN ('MINIMUM', 'MAXIMUM')),
    CONSTRAINT ck_product_control_parameter_type CHECK (data_type IN ('INTEGER', 'DECIMAL')),
    CONSTRAINT ck_product_control_parameter_order CHECK (display_order > 0)
);

INSERT INTO product_control_definition
    (id, code, title, evaluator_type, inquiry_code, default_failure_message, active) VALUES
    ('08200000-0000-0000-0000-000000000001', 'AGE_RANGE', 'محدوده سنی', 'AGE_RANGE', NULL, 'سن متقاضی در محدوده مجاز طرح نیست.', true),
    ('08200000-0000-0000-0000-000000000002', 'MINIMUM_CREDIT_RANK', 'حداقل رتبه اعتباری', 'MINIMUM_CREDIT_RANK', 'CREDIT_RATING_INQUIRY', 'رتبه اعتباری از حداقل طرح کمتر است.', true),
    ('08200000-0000-0000-0000-000000000003', 'NO_BAD_CHEQUE', 'فاقد چک برگشتی', 'NO_BAD_CHEQUE', 'BAD_CHEQUE_INQUIRY', 'چک برگشتی تسویه‌نشده وجود دارد.', true),
    ('08200000-0000-0000-0000-000000000004', 'MAXIMUM_BAD_CHEQUE_COUNT', 'حداکثر تعداد چک برگشتی', 'MAXIMUM_BAD_CHEQUE_COUNT', 'BAD_CHEQUE_INQUIRY', 'تعداد چک‌های برگشتی از حد مجاز بیشتر است.', true),
    ('08200000-0000-0000-0000-000000000005', 'MAXIMUM_BAD_CHEQUE_AMOUNT', 'حداکثر مبلغ چک برگشتی', 'MAXIMUM_BAD_CHEQUE_AMOUNT', 'BAD_CHEQUE_INQUIRY', 'مبلغ چک‌های برگشتی از حد مجاز بیشتر است.', true);

INSERT INTO product_control_parameter_definition
    (id, control_definition_id, code, title, value_role, data_type, required, display_order) VALUES
    ('08300000-0000-0000-0000-000000000001', '08200000-0000-0000-0000-000000000001', 'MINIMUM_AGE', 'حداقل سن', 'MINIMUM', 'INTEGER', false, 10),
    ('08300000-0000-0000-0000-000000000002', '08200000-0000-0000-0000-000000000001', 'MAXIMUM_AGE', 'حداکثر سن', 'MAXIMUM', 'INTEGER', false, 20),
    ('08300000-0000-0000-0000-000000000003', '08200000-0000-0000-0000-000000000002', 'MINIMUM_RANK', 'حداقل رتبه اعتباری', 'MINIMUM', 'INTEGER', true, 10),
    ('08300000-0000-0000-0000-000000000004', '08200000-0000-0000-0000-000000000004', 'MAXIMUM_COUNT', 'حداکثر تعداد چک برگشتی', 'MAXIMUM', 'INTEGER', true, 10),
    ('08300000-0000-0000-0000-000000000005', '08200000-0000-0000-0000-000000000005', 'MAXIMUM_AMOUNT', 'حداکثر مبلغ چک برگشتی', 'MAXIMUM', 'DECIMAL', true, 10);

CREATE TABLE product_plan_guarantor_policy (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    plan_id uuid NOT NULL UNIQUE,
    minimum_count integer NOT NULL,
    maximum_count integer NOT NULL,
    required boolean NOT NULL,
    requires_collateral boolean NOT NULL,
    enabled boolean NOT NULL,
    CONSTRAINT fk_product_plan_guarantor_policy_plan FOREIGN KEY (plan_id)
        REFERENCES product_catalog_plan(id) ON DELETE CASCADE,
    CONSTRAINT ck_product_plan_guarantor_count CHECK (
        minimum_count >= 0 AND maximum_count >= minimum_count
    ),
    CONSTRAINT ck_product_plan_guarantor_required CHECK (NOT required OR minimum_count > 0)
);

CREATE TABLE product_plan_collateral (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    plan_id uuid NOT NULL,
    collateral_type_id uuid NOT NULL,
    minimum_coverage_percent numeric(7, 2) NOT NULL,
    required boolean NOT NULL,
    enabled boolean NOT NULL,
    CONSTRAINT fk_product_plan_collateral_plan FOREIGN KEY (plan_id)
        REFERENCES product_catalog_plan(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_plan_collateral_type FOREIGN KEY (collateral_type_id)
        REFERENCES product_collateral_type(id),
    CONSTRAINT uk_product_plan_collateral_type UNIQUE (plan_id, collateral_type_id),
    CONSTRAINT ck_product_plan_collateral_coverage CHECK (minimum_coverage_percent >= 0)
);

CREATE TABLE product_plan_guarantor_collateral (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    plan_id uuid NOT NULL,
    collateral_type_id uuid NOT NULL,
    minimum_coverage_percent numeric(7, 2) NOT NULL,
    required boolean NOT NULL,
    enabled boolean NOT NULL,
    CONSTRAINT fk_product_guarantor_collateral_plan FOREIGN KEY (plan_id)
        REFERENCES product_catalog_plan(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_guarantor_collateral_type FOREIGN KEY (collateral_type_id)
        REFERENCES product_collateral_type(id),
    CONSTRAINT uk_product_guarantor_collateral_type UNIQUE (plan_id, collateral_type_id),
    CONSTRAINT ck_product_guarantor_collateral_coverage CHECK (minimum_coverage_percent >= 0)
);

CREATE TABLE product_plan_fee (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    plan_id uuid NOT NULL,
    fee_definition_id uuid NOT NULL,
    enabled boolean NOT NULL,
    CONSTRAINT fk_product_plan_fee_plan FOREIGN KEY (plan_id)
        REFERENCES product_catalog_plan(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_plan_fee_definition FOREIGN KEY (fee_definition_id)
        REFERENCES product_fee_definition(id),
    CONSTRAINT uk_product_plan_fee_definition UNIQUE (plan_id, fee_definition_id)
);

CREATE TABLE product_plan_control (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    plan_id uuid NOT NULL,
    control_definition_id uuid NOT NULL,
    subject_type varchar(20) NOT NULL,
    priority integer NOT NULL,
    enabled boolean NOT NULL,
    CONSTRAINT fk_product_plan_control_plan FOREIGN KEY (plan_id)
        REFERENCES product_catalog_plan(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_plan_control_definition FOREIGN KEY (control_definition_id)
        REFERENCES product_control_definition(id),
    CONSTRAINT uk_product_plan_control_definition UNIQUE (plan_id, subject_type, control_definition_id),
    CONSTRAINT uk_product_plan_control_priority UNIQUE (plan_id, subject_type, priority),
    CONSTRAINT ck_product_plan_control_priority CHECK (priority > 0),
    CONSTRAINT ck_product_plan_control_subject CHECK (subject_type IN ('APPLICANT', 'GUARANTOR'))
);

CREATE TABLE product_plan_control_parameter_value (
    id uuid PRIMARY KEY,
    version bigint NOT NULL DEFAULT 0,
    plan_control_id uuid NOT NULL,
    parameter_definition_id uuid NOT NULL,
    numeric_value numeric(19, 4) NOT NULL,
    CONSTRAINT fk_product_plan_control_value_control FOREIGN KEY (plan_control_id)
        REFERENCES product_plan_control(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_plan_control_value_definition FOREIGN KEY (parameter_definition_id)
        REFERENCES product_control_parameter_definition(id),
    CONSTRAINT uk_product_plan_control_parameter UNIQUE (plan_control_id, parameter_definition_id),
    CONSTRAINT ck_product_plan_control_parameter_value CHECK (numeric_value >= 0)
);

CREATE INDEX ix_product_plan_requirements
    ON product_plan_guarantor_policy(plan_id, enabled);

CREATE INDEX ix_product_plan_collaterals
    ON product_plan_collateral(plan_id, enabled);

CREATE INDEX ix_product_plan_guarantor_collaterals
    ON product_plan_guarantor_collateral(plan_id, enabled);

CREATE INDEX ix_product_plan_fees
    ON product_plan_fee(plan_id, enabled);

CREATE INDEX ix_product_plan_controls
    ON product_plan_control(plan_id, subject_type, enabled, priority);
