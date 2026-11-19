
CREATE FUNCTION require_modification_recorded_event()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.type = 'REMOVED'
        AND NOT EXISTS (
            SELECT 1
            FROM equipment_modification_events
            WHERE modification_id = NEW.modification_id
              AND type = 'RECORDED'
        )
    THEN
        RAISE EXCEPTION
            'Modification removal requires a RECORDED event';
    END IF;

    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_require_modification_recorded_event
    AFTER INSERT ON equipment_modification_events
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
EXECUTE FUNCTION require_modification_recorded_event();
