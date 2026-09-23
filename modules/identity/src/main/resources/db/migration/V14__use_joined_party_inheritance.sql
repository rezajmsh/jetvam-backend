ALTER TABLE iam_individual_party
    DROP COLUMN version,
    DROP COLUMN created_at,
    DROP COLUMN updated_at;

ALTER TABLE iam_organization_party
    DROP COLUMN version,
    DROP COLUMN created_at,
    DROP COLUMN updated_at;

CREATE FUNCTION iam_assert_party_subtype()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    actual_type varchar(30);
BEGIN
    SELECT party_type INTO actual_type
    FROM iam_party
    WHERE id = NEW.id;

    IF actual_type IS DISTINCT FROM TG_ARGV[0] THEN
        RAISE EXCEPTION 'Party % has type %, expected %', NEW.id, actual_type, TG_ARGV[0]
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_iam_individual_party_type
    BEFORE INSERT OR UPDATE ON iam_individual_party
    FOR EACH ROW EXECUTE FUNCTION iam_assert_party_subtype('INDIVIDUAL');

CREATE TRIGGER trg_iam_organization_party_type
    BEFORE INSERT OR UPDATE ON iam_organization_party
    FOR EACH ROW EXECUTE FUNCTION iam_assert_party_subtype('ORGANIZATION');

CREATE FUNCTION iam_prevent_party_type_change()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.party_type IS DISTINCT FROM NEW.party_type THEN
        RAISE EXCEPTION 'Party type cannot be changed after creation'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_iam_party_type_immutable
    BEFORE UPDATE OF party_type ON iam_party
    FOR EACH ROW EXECUTE FUNCTION iam_prevent_party_type_change();
