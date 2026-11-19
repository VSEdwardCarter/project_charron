
-- Validate timestamps on initial RECORDED events.

CREATE FUNCTION validate_modification_recorded_timestamps()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    modification_effective_at TIMESTAMPTZ;
    modification_recorded_at TIMESTAMPTZ;
BEGIN
    SELECT effective_at, recorded_at
    INTO modification_effective_at, modification_recorded_at
    FROM equipment_modifications
    WHERE id = NEW.modification_id
        FOR SHARE;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'Modification does not exist';
    END IF;

    IF NEW.effective_at <> modification_effective_at
        OR NEW.recorded_at <> modification_recorded_at
    THEN
        RAISE EXCEPTION
            'RECORDED event timestamps must match modification timestamps';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_modification_recorded_timestamps
    BEFORE INSERT ON equipment_modification_events
    FOR EACH ROW
    WHEN (NEW.type = 'RECORDED')
EXECUTE FUNCTION validate_modification_recorded_timestamps();


-- Protect original modification timestamps from later changes.

CREATE FUNCTION prevent_modification_timestamp_changes()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.effective_at IS DISTINCT FROM OLD.effective_at
        OR NEW.recorded_at IS DISTINCT FROM OLD.recorded_at
    THEN
        RAISE EXCEPTION
            'Original modification timestamps cannot be changed';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_prevent_modification_timestamp_changes
    BEFORE UPDATE OF effective_at, recorded_at
    ON equipment_modifications
    FOR EACH ROW
EXECUTE FUNCTION prevent_modification_timestamp_changes();
