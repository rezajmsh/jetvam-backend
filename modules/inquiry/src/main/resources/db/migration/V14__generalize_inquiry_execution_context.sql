ALTER TABLE inquiry_request
    ADD COLUMN execution_context_json TEXT;

UPDATE inquiry_request
SET execution_context_json = jsonb_build_object(
        'stage', 'POLL',
        'trackingCode', external_tracking_code
    )::text
WHERE external_tracking_code IS NOT NULL;

ALTER TABLE inquiry_request
    DROP COLUMN external_tracking_code;
