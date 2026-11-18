
CREATE OR REPLACE FUNCTION prevent_modification_event_mutation()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'Equipment modification events are immutable';
END;
$$;

CREATE TRIGGER trg_prevent_modification_event_update
    BEFORE UPDATE ON equipment_modification_events
    FOR EACH ROW
EXECUTE FUNCTION prevent_modification_event_mutation();

CREATE TRIGGER trg_prevent_modification_event_delete
    BEFORE DELETE ON equipment_modification_events
    FOR EACH ROW
EXECUTE FUNCTION prevent_modification_event_mutation();
