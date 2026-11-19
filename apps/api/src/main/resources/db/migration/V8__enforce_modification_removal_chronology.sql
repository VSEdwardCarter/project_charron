
CREATE FUNCTION validate_modification_removal_chronology()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
DECLARE
    installation_time TIMESTAMPTZ;
BEGIN
    IF NEW.type = 'REMOVED' THEN

        SELECT effective_at
        INTO installation_time
        FROM equipment_modifications
        WHERE id = NEW.modification_id
            FOR SHARE;

        IF installation_time IS NULL THEN
            RAISE EXCEPTION
                'Modification must exist before removal';
        END IF;

        IF NEW.effective_at < installation_time THEN
            RAISE EXCEPTION
                'Modification removal cannot precede installation';
        END IF;

    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_modification_removal_chronology
    BEFORE INSERT ON equipment_modification_events
    FOR EACH ROW
EXECUTE FUNCTION validate_modification_removal_chronology();
