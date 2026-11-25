CREATE OR REPLACE FUNCTION enforce_manifest_lifecycle_sequence()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    parent_movement_id UUID;
    manifest_version INTEGER;
    manifest_generated_at TIMESTAMPTZ;
    current_status VARCHAR(30);
    last_transition_at TIMESTAMPTZ;
BEGIN

    -- Identify the parent movement and manifest version.
    SELECT
        movement_id,
        version,
        generated_at
    INTO
        parent_movement_id,
        manifest_version,
        manifest_generated_at
    FROM movement_manifests
    WHERE id = NEW.manifest_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'Referenced manifest does not exist'
            USING ERRCODE = '23503';
    END IF;

    -- Acquire the movement lock first, consistent with
    -- the application service locking strategy.
    PERFORM id
    FROM movements
    WHERE id = parent_movement_id
        FOR UPDATE;

    -- Acquire the individual manifest lock second.
    PERFORM id
    FROM movement_manifests
    WHERE id = NEW.manifest_id
        FOR UPDATE;

    -- Preserve manifest generation chronology.
    IF NEW.recorded_at < manifest_generated_at THEN
        RAISE EXCEPTION
            'Lifecycle event cannot precede manifest generation'
            USING ERRCODE = '23514';
    END IF;

    -- Determine the current manifest lifecycle state.
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

    -- Enforce movement-level issuance constraints.
    IF NEW.new_status = 'ISSUED' THEN

        -- Reject issuance of an older version if any newer
        -- version has previously been issued.
        IF EXISTS (
            SELECT 1
            FROM movement_manifests candidate
                     JOIN manifest_lifecycle_events issuance
                          ON issuance.manifest_id = candidate.id
                              AND issuance.new_status = 'ISSUED'
            WHERE candidate.movement_id = parent_movement_id
              AND candidate.version > manifest_version
        ) THEN

            RAISE EXCEPTION
                'Cannot issue an older manifest version after a newer version has been issued'
                USING ERRCODE = '23514';

        END IF;

        -- Reject issuance when another manifest remains issued.
        IF EXISTS (
            SELECT 1
            FROM movement_manifests candidate
                     JOIN manifest_lifecycle_events issuance
                          ON issuance.manifest_id = candidate.id
                              AND issuance.new_status = 'ISSUED'
            WHERE candidate.movement_id = parent_movement_id
              AND candidate.id <> NEW.manifest_id
              AND NOT EXISTS (
                SELECT 1
                FROM manifest_lifecycle_events supersession
                WHERE supersession.manifest_id = candidate.id
                  AND supersession.new_status = 'SUPERSEDED'
            )
        ) THEN

            RAISE EXCEPTION
                'Another issued manifest already exists for this movement'
                USING ERRCODE = '23514';

        END IF;

    END IF;

    RETURN NEW;

END;
$$;