CREATE OR REPLACE FUNCTION enforce_manifest_lifecycle_sequence()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    manifest_generated_at TIMESTAMPTZ;
    current_status VARCHAR(30);
    last_transition_at TIMESTAMPTZ;
BEGIN

    SELECT generated_at
    INTO manifest_generated_at
    FROM movement_manifests
    WHERE id = NEW.manifest_id
        FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'Referenced manifest does not exist'
            USING ERRCODE = '23503';
    END IF;

    IF NEW.recorded_at < manifest_generated_at THEN
        RAISE EXCEPTION
            'Lifecycle event cannot precede manifest generation'
            USING ERRCODE = '23514';
    END IF;

    SELECT
        new_status,
        recorded_at
    INTO
        current_status,
        last_transition_at
    FROM manifest_lifecycle_events
    WHERE manifest_id = NEW.manifest_id
    ORDER BY
        recorded_at DESC,
        CASE new_status
            WHEN 'REVIEWED' THEN 1
            WHEN 'ISSUED' THEN 2
            WHEN 'SUPERSEDED' THEN 3
            END DESC
    LIMIT 1;

    IF NOT FOUND THEN
        current_status := 'DRAFT';
        last_transition_at := manifest_generated_at;
    END IF;

    IF NEW.previous_status <> current_status THEN
        RAISE EXCEPTION
            'Invalid manifest lifecycle sequence: expected %, received %',
            current_status,
            NEW.previous_status
            USING ERRCODE = '23514';
    END IF;

    IF NEW.recorded_at < last_transition_at THEN
        RAISE EXCEPTION
            'Lifecycle event cannot precede the previous transition'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;

END;
$$;


CREATE TRIGGER trg_enforce_manifest_lifecycle_sequence
    BEFORE INSERT ON manifest_lifecycle_events
    FOR EACH ROW
EXECUTE FUNCTION enforce_manifest_lifecycle_sequence();